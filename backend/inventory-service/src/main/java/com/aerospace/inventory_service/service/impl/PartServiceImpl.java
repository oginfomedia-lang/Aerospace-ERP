package com.aerospace.inventory_service.service.impl;

import com.aerospace.inventory_service.dto.*;
import com.aerospace.inventory_service.entity.*;
import com.aerospace.inventory_service.event.*;
import com.aerospace.inventory_service.event.producer.InventoryProducer;
import com.aerospace.inventory_service.exception.ResourceNotFoundException;
import com.aerospace.inventory_service.mapper.PartMapper;
import com.aerospace.inventory_service.repository.*;
import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.common.rules.RuleEngine;
import com.aerospace.inventory_service.service.LocationService;
import com.aerospace.inventory_service.service.PartService;
import com.aerospace.inventory_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartServiceImpl implements PartService {

    private final PartRepository partRepository;
    private final StockBalanceRepository balanceRepository;
    private final TransactionService transactionService;
    private final PartMapper mapper;
    private final UomConversionRepository uomRepository;
    private final InventoryProducer inventoryProducer;
    private final DocumentSequenceService documentSequenceService;
    private final NcrRepository ncrRepository;
    private final LocationService locationService;
    private final MaterialIssueRecordRepository materialIssueRecordRepository;
    private final RuleEngine ruleEngine;

    // ── CREATE PART ──────────────────────────────────────────────────────────
    // Kafka: PART_CREATED → notification-service logs "New Part Registered"

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PartResponseDTO createPart(PartRequestDTO dto) {
        if (partRepository.findBySkuCode(dto.getSerialNumber()).isPresent()) {
            throw new RuntimeException("Part with this SKU/Serial already exists");
        }
        // New parts fall back to the entity defaults (batchRequired=true, others false) when unset
        validateItemMasterRules(
                dto.getBatchRequired() != null ? dto.getBatchRequired() : true,
                dto.getSerialRequired() != null ? dto.getSerialRequired() : false,
                dto.getExpiryTracking() != null ? dto.getExpiryTracking() : false,
                dto.getShelfLifeDays(),
                dto.getMinStock(), dto.getReorderLevel(), dto.getMaxStock());
        Part part = mapper.toEntity(dto);
        Part saved = partRepository.save(part);

        try {
            inventoryProducer.sendCreatedEvent(
                PartCreatedEvent.builder()
                    .partId(saved.getId())
                    .partName(saved.getName())
                    .id(saved.getId())           // legacy field
                    .name(saved.getName())       // legacy field
                    .serialNumber(dto.getSerialNumber()) // legacy field
                    .batchNumber("")
                    .initialQuantity(0)
                    .reorderLevel(saved.getReorderLevel() != null ? saved.getReorderLevel() : 0)
                    .location("")
                    .createdBy("System")
                    .build()
            );
        } catch (Exception ex) {
            log.warn("[KAFKA] PART_CREATED publish failed for partId={}: {}", saved.getId(), ex.getMessage());
        }

        return mapper.toDTO(saved);
    }

    // ── GET BY ID ────────────────────────────────────────────────────────────

    @Override
    public PartResponseDTO getById(Long id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found with ID: " + id));
        return mapWithLiveStock(part, StockStatus.APPROVED);
    }

    @Override
    public PartResponseDTO getBySkuCode(String skuCode) {
        Part part = partRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found with SKU: " + skuCode));
        return mapWithLiveStock(part, StockStatus.APPROVED);
    }

    // ── ADD STOCK (inward-from-grn) ──────────────────────────────────────────
    // Kafka: STOCK_RECEIVED → notification-service logs "Stock Received"

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PartResponseDTO addStock(Long id, int quantity, String ignoredManualBatchNumber, String loc,
                                    LocalDateTime expiryDate,
                                    String ignoredManualGrnNumber, String challanNumber,
                                    String supplierName, String transportName,
                                    Integer inspectionQty, String poNumber) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        if (quantity <= 0) {
            throw new IllegalArgumentException("Received quantity must be greater than zero.");
        }
        if (part.isExpiryTracking() && expiryDate == null) {
            throw new IllegalArgumentException("ITM-BR-003: Expiry date is required for this item (expiry tracking enabled).");
        }
        if (loc != null && !loc.isBlank()) {
            locationService.assertActive(loc);
        }

        // GRN-FR-001: the GRN number is always system-generated — manual/free-text numbers are ignored.
        String systemGrnNumber = documentSequenceService.generate("GRN");
        // GRN-FR-004: batch number is always system-generated too — every physical receipt
        // (whether from a PO/GRN or a manual stock intake) becomes its own distinct lot.
        // Manual/free-text batch numbers are never accepted, same philosophy as the GRN number above.
        String batch = documentSequenceService.generate("BATCH");

        // GRN-FR-003: route to the QC bucket only when the item actually requires inspection;
        // otherwise the received stock is available immediately.
        StockStatus targetStatus = part.isQcRequired() ? StockStatus.PENDING_QA : StockStatus.APPROVED;

        // The batch number is freshly generated above, so every receipt always becomes its
        // own new StockBalance row — no merge-lookup against an existing batch is needed.
        StockBalance balance = StockBalance.builder()
                .partId(id).batchNumber(batch)
                .status(targetStatus).quantity(quantity)
                .build();
        balance.setLocation(loc);
        balance.setExpiryDate(expiryDate);
        if (poNumber != null && !poNumber.isBlank()) {
            balance.setPoNumber(poNumber);
        }
        balanceRepository.save(balance);

        // Build GRN traceability remark (Step 1+2 of GRN flow)
        String grnRemark = buildGrnRemark(systemGrnNumber, challanNumber, supplierName, transportName,
                quantity, inspectionQty, targetStatus);
        transactionService.logTransaction(id, batch, "GRN_RECEIPT", quantity, systemGrnNumber, grnRemark);

        try {
            inventoryProducer.sendStockReceivedEvent(
                StockReceivedEvent.builder()
                    .partId(id)
                    .partName(part.getName())
                    .batchNumber(batch)
                    .quantityReceived(quantity)
                    .supplier("GRN")
                    .location(loc != null ? loc : "")
                    .receivedBy("System")
                    .build()
            );
        } catch (Exception ex) {
            log.warn("[KAFKA] STOCK_RECEIVED publish failed for partId={}: {}", id, ex.getMessage());
        }

        if (targetStatus == StockStatus.APPROVED) {
            checkAndPublishLowStock(part, totalApprovedStock(id), batch, loc);
        }

        PartResponseDTO response = mapper.toDTO(part);
        response.setQuantity(balance.getQuantity());
        response.setBatchNumber(batch);
        response.setLocation(loc);
        response.setExpiryDate(expiryDate);
        response.setStatus(targetStatus.name());
        response.setGrnNumber(systemGrnNumber);
        return response;
    }

    // ── ISSUE TO PRODUCTION ──────────────────────────────────────────────────
    // Kafka: MATERIAL_ISSUED + (optionally) STOCK_LOW

    @Override
    @Transactional
    public PartResponseDTO issueToProduction(Long id, int quantity, String batch,
                                              Long workOrderId, String woNumber, Long lineItemId) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        if (quantity <= 0) {
            throw new IllegalArgumentException("Issue quantity must be greater than zero.");
        }

        List<StockBalance> candidates;
        if (batch != null && !batch.isBlank()) {
            // Explicit batch requested — issue only from that one batch, no FIFO
            // spillover (e.g. a traceability requirement pinned to a specific batch).
            List<StockBalance> batchBalances = balanceRepository.findAllByPartIdAndBatchNumberAndStatusOrderByIdAsc(id, batch, StockStatus.APPROVED);
            if(batchBalances.isEmpty()) {
                throw new RuntimeException("No APPROVED stock for batch: "+ batch);
            }
            candidates = batchBalances;

        } else {
            // FIFO: consume the oldest APPROVED batches first. A StockBalance row is
            // only ever created once, on a batch's first receipt (later receipts of
            // the same batch just increment its quantity) — so ordering by id gives
            // us oldest-batch-first without needing a separate received-date column.
            candidates = balanceRepository.findAllBalancesByPart(id).stream()
                    .filter(b -> b.getStatus() == StockStatus.APPROVED
                            && b.getQuantity() != null && b.getQuantity() > 0)
                    .sorted(java.util.Comparator.comparing(StockBalance::getId))
                    .toList();
        }

        int totalAvailable = candidates.stream()
                .mapToInt(b -> b.getQuantity() != null ? b.getQuantity() : 0)
                .sum();
        if (totalAvailable < quantity) {
            throw new RuntimeException("Insufficient approved stock: requested " + quantity
                    + ", available " + totalAvailable
                    + (batch != null && !batch.isBlank() ? " in batch " + batch : " across all APPROVED batches"));
        }

        int remainingToIssue = quantity;
        List<java.util.Map.Entry<StockBalance, Integer>> allocations = new java.util.ArrayList<>();
        for (StockBalance b : candidates) {
            if (remainingToIssue <= 0) break;
            int take = Math.min(remainingToIssue, b.getQuantity());
            allocations.add(java.util.Map.entry(b, take));
            remainingToIssue -= take;
        }

        IssueLoopResult result = deductAndBuildLines(id, allocations,
                candidates.size() > 1 ? "FIFO — part of a " + quantity + "-unit issue" : "Material Issued");

        return finalizeIssuance(id, part, quantity, result, workOrderId, woNumber, lineItemId,
                "Production", "System");
    }

    // Explicit multi-batch allocation — shares the deduct/log/record loop and the
    // MaterialIssueRecord/Kafka/response tail with issueToProduction above; only how
    // the (StockBalance, quantity) pairs are resolved differs.
    @Override
    @Transactional
    public PartResponseDTO issueToProductionWithAllocation(Long id, List<BatchAllocationRequest> requestedAllocations,
                                                            Long workOrderId, String woNumber, Long lineItemId,
                                                            String issuedTo, String issuedBy) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        if (requestedAllocations == null || requestedAllocations.isEmpty()) {
            throw new IllegalArgumentException("At least one batch allocation is required.");
        }

        List<java.util.Map.Entry<StockBalance, Integer>> allocations = new java.util.ArrayList<>();
        int totalQuantity = 0;
        for (BatchAllocationRequest alloc : requestedAllocations) {
            if (alloc.getQuantity() == null || alloc.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Allocation quantity must be greater than zero for batch " + alloc.getBatchNumber());
            }
            // A batch can legitimately hold APPROVED stock in more than one row (split across
            // locations by a bin-to-bin transfer) — sum across all of them and spread the
            // deduction across whichever rows actually have quantity, instead of assuming the
            // first row found (which may be an emptied-out one) has enough on its own.
            List<StockBalance> balances = balanceRepository
                    .findAllByPartIdAndBatchNumberAndStatusOrderByIdAsc(id, alloc.getBatchNumber(), StockStatus.APPROVED);

            if(balances.isEmpty()){
                throw new RuntimeException("No APPROVED stock for batch: "+ alloc.getBatchNumber());
            }

            int availableForBatch = balances.stream()
                    .mapToInt(b -> b.getQuantity() != null ? b.getQuantity() : 0)
                    .sum();
            if (availableForBatch < alloc.getQuantity()) {
                throw new RuntimeException("Requested " + alloc.getQuantity() + " from batch "
                        + alloc.getBatchNumber() + " but only " + availableForBatch + " is APPROVED and available.");
            }

            int remaining = alloc.getQuantity();
            for (StockBalance b : balances) {
                if (remaining <= 0) break;
                int have = b.getQuantity() != null ? b.getQuantity() : 0;
                if (have <= 0) continue;
                int take = Math.min(remaining, have);
                allocations.add(java.util.Map.entry(b, take));
                remaining -= take;
            }
            totalQuantity += alloc.getQuantity();
        }

        IssueLoopResult result = deductAndBuildLines(id, allocations, "Manual multi-batch allocation");

        return finalizeIssuance(id, part, totalQuantity, result, workOrderId, woNumber, lineItemId,
                issuedTo != null && !issuedTo.isBlank() ? issuedTo : "Production",
                issuedBy != null && !issuedBy.isBlank() ? issuedBy : "System");
    }

    // Shared by both issuance paths: deducts each (balance, amount) pair, logs a
    // PRODUCTION_ISSUE transaction per batch touched, and builds the structured
    // MaterialIssueRecordLine rows for the printable receipt.
    private record IssueLoopResult(String batchSummary, String lastLocation, List<MaterialIssueRecordLine> issueLines) {}

    private IssueLoopResult deductAndBuildLines(Long partId, List<java.util.Map.Entry<StockBalance, Integer>> allocations,
                                                 String multiBatchTransactionLabel) {
        StringBuilder batchSummary = new StringBuilder();
        String lastLocation = null;
        List<MaterialIssueRecordLine> issueLines = new java.util.ArrayList<>();
        boolean multiBatch = allocations.size() > 1;

        for (java.util.Map.Entry<StockBalance, Integer> alloc : allocations) {
            StockBalance b = alloc.getKey();
            int take = alloc.getValue();
            b.setQuantity(b.getQuantity() - take);
            balanceRepository.save(b);
            lastLocation = b.getLocation();

            transactionService.logTransaction(partId, b.getBatchNumber(), "PRODUCTION_ISSUE", -take, null,
                    multiBatch ? "Material Issued (" + multiBatchTransactionLabel + ")" : "Material Issued");

            if (batchSummary.length() > 0) batchSummary.append(", ");
            batchSummary.append(b.getBatchNumber()).append(":").append(take);

            issueLines.add(MaterialIssueRecordLine.builder()
                    .batchNumber(b.getBatchNumber())
                    .quantityTaken(take)
                    .location(b.getLocation())
                    .expiryDate(b.getExpiryDate())
                    .build());
        }
        return new IssueLoopResult(batchSummary.toString(), lastLocation, issueLines);
    }

    // Shared tail: persist the MaterialIssueRecord, publish MaterialIssuedEvent +
    // (optionally) StockLowEvent, and build the response DTO.
    private PartResponseDTO finalizeIssuance(Long id, Part part, int totalQuantity, IssueLoopResult result,
                                              Long workOrderId, String woNumber, Long lineItemId,
                                              String issuedTo, String issuedBy) {
        MaterialIssueRecord issueRecord = MaterialIssueRecord.builder()
                .issueNumber(documentSequenceService.generate("ISS"))
                .partId(id)
                .partName(part.getName())
                .totalQuantityIssued(totalQuantity)
                .workOrderId(workOrderId)
                .woNumber(woNumber)
                .lineItemId(lineItemId)
                .issuedTo(issuedTo)
                .issuedBy(issuedBy)
                .build();
        for (MaterialIssueRecordLine line : result.issueLines()) {
            line.setMaterialIssueRecord(issueRecord);
        }
        issueRecord.setLines(result.issueLines());
        final MaterialIssueRecord savedIssueRecord = materialIssueRecordRepository.save(issueRecord);

        // Total remaining APPROVED stock across ALL batches for this part — low-stock
        // and the reported "on hand" quantity must reflect the whole part, not just
        // whichever single batch happened to be touched last.
        int totalRemainingApproved = balanceRepository.findAllBalancesByPart(id).stream()
                .filter(b -> b.getStatus() == StockStatus.APPROVED)
                .mapToInt(b -> b.getQuantity() != null ? b.getQuantity() : 0)
                .sum();

        try {
            inventoryProducer.sendMaterialIssuedEvent(
                MaterialIssuedEvent.builder()
                    .workOrderId(workOrderId)
                    .woNumber(woNumber)
                    .lineItemId(lineItemId)
                    .partId(id)
                    .partName(part.getName())
                    .batchNumber(result.batchSummary())
                    .quantityIssued(totalQuantity)
                    .remainingQuantity(totalRemainingApproved)
                    .issuedTo(issuedTo)
                    .issuedBy(issuedBy)
                    .location(result.lastLocation() != null ? result.lastLocation() : "")
                    .build()
            );
        } catch (Exception ex) {
            log.warn("[KAFKA] MATERIAL_ISSUED publish failed for partId={}: {}", id, ex.getMessage());
        }

        checkAndPublishLowStock(part, totalRemainingApproved, result.batchSummary(), result.lastLocation());

        PartResponseDTO response = mapper.toDTO(part);
        response.setQuantity(totalRemainingApproved);
        response.setBatchNumber(result.batchSummary());
        response.setStatus(StockStatus.APPROVED.name());
        response.setMaterialIssueRecordId(savedIssueRecord.getId());
        response.setIssueNumber(savedIssueRecord.getIssueNumber());
        return response;
    }

    // ── PROCESS QA DECISION ──────────────────────────────────────────────────
    // Kafka: QA_STATUS_CHANGED → notification-service logs QA_APPROVED / QA_REJECTED

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PartResponseDTO processQADecision(Long id, String batch, Integer approvedQty, Integer rejectedQty,
                                              String criteria, String remarks, Set<String> actorRoles) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        StockBalance pending = balanceRepository
                .findByPartIdAndBatchNumberAndStatus(id, batch, StockStatus.PENDING_QA)
                .orElseThrow(() -> new ResourceNotFoundException("Pending stock not found for batch: " + batch));

        int approved = approvedQty != null ? approvedQty : 0;
        int rejected = rejectedQty != null ? rejectedQty : 0;
        int pendingQty = pending.getQuantity();
        if (approved + rejected != pendingQty) {
            throw new IllegalArgumentException("Approved (" + approved + ") + rejected (" + rejected
                    + ") must equal the pending quantity (" + pendingQty + ") for batch " + batch + ".");
        }

        String loc = pending.getLocation();
        LocalDateTime expiry = pending.getExpiryDate();
        String poNumber = pending.getPoNumber();
        balanceRepository.delete(pending);

        String newStatus = rejected == 0 ? "APPROVED" : approved == 0 ? "REJECTED" : "PARTIALLY_APPROVED";

        if (approved > 0) {
            Set<String> requiredRoles = ruleEngine.getRequiredRoles("QA_RELEASE", Map.of("criticality", part.getCriticality()));
            if (!requiredRoles.isEmpty() && Collections.disjoint(requiredRoles, actorRoles)) {
                throw new IllegalArgumentException("Releasing a " + part.getCriticality()
                        + "-criticality part requires one of: " + requiredRoles + ".");
            }
            moveToStatus(id, batch, StockStatus.APPROVED, approved, loc, expiry, poNumber);
        }
        String ncrNumber = null;
        if (rejected > 0) {
            moveToStatus(id, batch, StockStatus.REJECTED, rejected, loc, expiry, poNumber);

            // GRN-FR / aerospace QC convention: a rejected batch doesn't just sit blocked —
            // it needs a Non-Conformance Report so it gets dispositioned (return to vendor,
            // scrap, or use-as-is under special approval) instead of staying REJECTED forever.
            ncrNumber = documentSequenceService.generate("NCR");
            ncrRepository.save(NonConformanceReport.builder()
                    .ncrNumber(ncrNumber)
                    .partId(id)
                    .partName(part.getName())
                    .batchNumber(batch)
                    .quantity(rejected)
                    .poNumber(poNumber)
                    .reason(remarks)
                    .status(NcrStatus.OPEN)
                    .build());
            log.info("[NCR] {} auto-created for rejected batch {} (partId={}, qty={})",
                    ncrNumber, batch, id, rejected);
        }

        transactionService.logTransaction(id, batch, "QA_DECISION", 0, null,
                "Moved to " + newStatus + " (approved=" + approved + ", rejected=" + rejected + "): "
                        + (criteria != null && !criteria.isBlank() ? "[" + criteria + "] " : "") + remarks);

        try {
            boolean expired = expiry != null && expiry.isBefore(LocalDateTime.now());
            inventoryProducer.sendQAStatusChangedEvent(
                QAStatusChangedEvent.builder()
                    .partId(id)
                    .partName(part.getName())
                    .batchNumber(batch)
                    .newStatus(newStatus)
                    .decision(newStatus)
                    .status(newStatus)           // legacy field
                    .inspectorName("QA Inspector")
                    .location(loc != null ? loc : "")
                    .remarks(remarks != null ? remarks : "")
                    .batchExpired(expired)
                    .expiryDate(expiry != null ? expiry.toString() : "")
                    .poNumber(poNumber)
                    .skuCode(part.getSkuCode())
                    .quantity(pendingQty)
                    .approvedQty(approved)
                    .rejectedQty(rejected)
                    .ncrNumber(ncrNumber)
                    .build()
            );
        } catch (Exception ex) {
            log.warn("[KAFKA] QA_STATUS_CHANGED publish failed for partId={}: {}", id, ex.getMessage());
        }

        // A rejection means less of this batch became available than was pending — the
        // part's total APPROVED stock can drop below reorder level here just as easily as
        // from a material issue, so the same check must run on this path too.
        checkAndPublishLowStock(part, totalApprovedStock(id), batch, loc);

        PartResponseDTO response = mapper.toDTO(part);
        response.setQuantity(approved);
        response.setBatchNumber(batch);
        response.setStatus(newStatus);
        response.setQaRemarks(remarks);
        response.setExpiryDate(expiry);
        return response;
    }

    // Adds `qty` to the target status bucket for this part/batch, creating it if absent —
    // shared by both the approved and rejected sides of a (possibly partial) QA split.
    private void moveToStatus(Long partId, String batch, StockStatus status, int qty,
                              String location, LocalDateTime expiry, String poNumber) {
        // If this batch already has more than one row at this status (e.g. it was split
        // across locations earlier), just add onto the first one rather than throwing —
        // any existing bucket is a valid place to land this quantity.
        List<StockBalance> existing = balanceRepository
                .findAllByPartIdAndBatchNumberAndStatusOrderByIdAsc(partId, batch, status);
        StockBalance target = !existing.isEmpty() ? existing.get(0)
                : StockBalance.builder()
                        .partId(partId).batchNumber(batch)
                        .status(status).quantity(0).build();
        target.setQuantity(target.getQuantity() + qty);
        target.setLocation(location);
        target.setExpiryDate(expiry);
        if (poNumber != null && !poNumber.isBlank()) {
            target.setPoNumber(poNumber);
        }
        balanceRepository.save(target);
    }

    // Convenience overload — resolves the full pending quantity to one side, for callers
    // (the manual admin override endpoint) that only have a single status, not a split.
    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PartResponseDTO processQADecision(Long id, String batch, String newStatus, String remarks, Set<String> actorRoles) {
        StockBalance pending = balanceRepository
                .findByPartIdAndBatchNumberAndStatus(id, batch, StockStatus.PENDING_QA)
                .orElseThrow(() -> new ResourceNotFoundException("Pending stock not found for batch: " + batch));
        int qty = pending.getQuantity();
        boolean approved = StockStatus.APPROVED.name().equalsIgnoreCase(newStatus);
        return processQADecision(id, batch, approved ? qty : 0, approved ? 0 : qty, null, remarks, actorRoles);
    }

    // ── REST QUERIES ─────────────────────────────────────────────────────────

    @Override
    public List<PartResponseDTO> getAllParts() {
        return partRepository.findAll().stream().map(p -> mapWithLiveStock(p, null)).toList();
    }

    @Override
    public Page<PartResponseDTO> getAllParts(Pageable pageable) {
        return partRepository.findAll(pageable).map(p -> mapWithLiveStock(p, null));
    }

    @Override
    public List<PartResponseDTO> searchByName(String name) {
        return partRepository.findByNameContainingIgnoreCase(name).stream()
                .map(p -> mapWithLiveStock(p, null)).toList();
    }

    @Override
    public List<InventoryTransaction> getTransactionHistory(Long partId) {
        return transactionService.getByPartId(partId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void deletePart(Long id) {
        Part part = partRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Part not found with ID: "+id));
        part.setStatus("INACTIVE");
        partRepository.save(part);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PartResponseDTO updatePart(Long id, PartRequestDTO dto) {
        log.info("ERP: Updating material master for id: {}", id);
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found"));

        // Validate against the EFFECTIVE state (existing values merged with incoming overrides),
        // since this is a partial update and either side of a rule may be omitted from the DTO.
        boolean effBatchRequired = dto.getBatchRequired() != null ? dto.getBatchRequired() : part.isBatchRequired();
        boolean effSerialRequired = dto.getSerialRequired() != null ? dto.getSerialRequired() : part.isSerialRequired();
        boolean effExpiryTracking = dto.getExpiryTracking() != null ? dto.getExpiryTracking() : part.isExpiryTracking();
        Integer effShelfLifeDays = dto.getShelfLifeDays() != null ? dto.getShelfLifeDays() : part.getShelfLifeDays();
        Integer effMinStock = dto.getMinStock() != null ? dto.getMinStock() : part.getMinStock();
        Integer effReorderLevel = dto.getReorderLevel() != null ? dto.getReorderLevel() : part.getReorderLevel();
        Integer effMaxStock = dto.getMaxStock() != null ? dto.getMaxStock() : part.getMaxStock();
        validateItemMasterRules(effBatchRequired, effSerialRequired, effExpiryTracking,
                effShelfLifeDays, effMinStock, effReorderLevel, effMaxStock);

        part.setName(dto.getName());
        part.setPartType(dto.getPartType());
        part.setCriticality(dto.getCriticality());
        part.setReorderLevel(dto.getReorderLevel());
        part.setSkuCode(dto.getSerialNumber());
        if (dto.getMinStock() != null) part.setMinStock(dto.getMinStock());
        if (dto.getMaxStock() != null) part.setMaxStock(dto.getMaxStock());
        if (dto.getSafetyStock() != null) part.setSafetyStock(dto.getSafetyStock());
        if (dto.getLeadTimeDays() != null) part.setLeadTimeDays(dto.getLeadTimeDays());
        if (dto.getShelfLifeDays() != null) part.setShelfLifeDays(dto.getShelfLifeDays());
        if (dto.getItemStatus() != null) part.setStatus(dto.getItemStatus());
        if (dto.getBatchRequired() != null) part.setBatchRequired(dto.getBatchRequired());
        if (dto.getSerialRequired() != null) part.setSerialRequired(dto.getSerialRequired());
        if (dto.getExpiryTracking() != null) part.setExpiryTracking(dto.getExpiryTracking());
        if (dto.getQcRequired() != null) part.setQcRequired(dto.getQcRequired());
        if (dto.getPreferredSupplierId() != null) part.setPreferredSupplierId(dto.getPreferredSupplierId());
        if (dto.getUnitPrice() != null) part.setUnitPrice(dto.getUnitPrice());
        if (dto.getTaxRatePercent() != null) part.setTaxRatePercent(dto.getTaxRatePercent());
        if (dto.getHsnCode() != null) part.setHsnCode(dto.getHsnCode());
        if (dto.getDescription() != null) part.setDescription(dto.getDescription());

        Part saved = partRepository.save(part);
        checkAndPublishLowStock(saved, totalApprovedStock(saved.getId()), null, null);
        return mapWithLiveStock(saved, null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void reserveStock(Long partId, String batch, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reserve quantity must be greater than zero.");
        }
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));
        // Same multi-location caveat as issueToProductionWithAllocation — a batch's APPROVED
        // stock can be spread across more than one StockBalance row (bin-to-bin transfer), so
        // resolve against all of them rather than a single-result lookup that throws once a
        // second row exists.
        List<StockBalance> approvedBalances = balanceRepository
                .findAllByPartIdAndBatchNumberAndStatusOrderByIdAsc(partId, batch, StockStatus.APPROVED);
        int totalApproved = approvedBalances.stream()
                .mapToInt(b -> b.getQuantity() != null ? b.getQuantity() : 0).sum();
        if (approvedBalances.isEmpty()) {
            throw new RuntimeException("Approved stock not found");
        }
        if (totalApproved < quantity)
            throw new RuntimeException("Insufficient stock to reserve");

        int remaining = quantity;
        for (StockBalance b : approvedBalances) {
            if (remaining <= 0) break;
            int have = b.getQuantity() != null ? b.getQuantity() : 0;
            if (have <= 0) continue;
            int take = Math.min(remaining, have);
            b.setQuantity(have - take);
            balanceRepository.save(b);
            remaining -= take;
        }

        List<StockBalance> reservedBalances = balanceRepository
                .findAllByPartIdAndBatchNumberAndStatusOrderByIdAsc(partId, batch, StockStatus.RESERVED);
        StockBalance reservedStock = !reservedBalances.isEmpty() ? reservedBalances.get(0)
                : StockBalance.builder()
                        .partId(partId).batchNumber(batch)
                        .status(StockStatus.RESERVED).quantity(0).build();

        reservedStock.setExpiryDate(approvedBalances.get(0).getExpiryDate());
        reservedStock.setQuantity(reservedStock.getQuantity() + quantity);
        balanceRepository.save(reservedStock);

        // Net quantity for this batch is unchanged — it's moving between buckets
        // (APPROVED -> RESERVED), not entering or leaving stock. Log 0, same as
        // QA_DECISION, so the ledger's running balance stays accurate.
        transactionService.logTransaction(partId, batch, "STOCK_RESERVATION",
                0, null, "Reserved for Work Order (" + quantity + " units)");

        // Reserving takes stock out of the AVAILABLE (APPROVED) bucket just as materially as
        // issuing it does — total APPROVED can cross the reorder threshold here too.
        checkAndPublishLowStock(part, totalApprovedStock(partId), batch,
                approvedBalances.get(0).getLocation());
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void releaseReservation(Long partId, String batch, int quantity){
        if(quantity <= 0) {
            throw new IllegalArgumentException("Release quantity must be greater than zero");
        }
        // Same multi-location caveat as reserveStock/issueToProductionWithAllocation.
        List<StockBalance> reservedBalances = balanceRepository
                .findAllByPartIdAndBatchNumberAndStatusOrderByIdAsc(partId, batch, StockStatus.RESERVED);
        int totalReserved = reservedBalances.stream()
                .mapToInt(b -> b.getQuantity() != null ? b.getQuantity() : 0).sum();
        if (reservedBalances.isEmpty()) {
            throw new RuntimeException("Reserved stock not found for batch:" + batch);
        }
        if(totalReserved < quantity) {
            throw new RuntimeException("Cannot release " + quantity + " units - only" +  totalReserved + " cirrently RESERVED for batch " + batch);
        }

        int remaining = quantity;
        for (StockBalance b : reservedBalances) {
            if (remaining <= 0) break;
            int have = b.getQuantity() != null ? b.getQuantity() : 0;
            if (have <= 0) continue;
            int take = Math.min(remaining, have);
            b.setQuantity(have - take);
            balanceRepository.save(b);
            remaining -= take;
        }

        List<StockBalance> approvedBalances = balanceRepository
                .findAllByPartIdAndBatchNumberAndStatusOrderByIdAsc(partId, batch, StockStatus.APPROVED);
        StockBalance approvedStock = !approvedBalances.isEmpty() ? approvedBalances.get(0)
                : StockBalance.builder()
                        .partId(partId).batchNumber(batch)
                        .status(StockStatus.APPROVED).quantity(0).build();

        approvedStock.setExpiryDate(reservedBalances.get(0).getExpiryDate());
        approvedStock.setQuantity(approvedStock.getQuantity() + quantity);
        balanceRepository.save(approvedStock);

        //netquantity unchanges - moving RESERVED -> APPROVED, same zero-log convention
        transactionService.logTransaction(partId, batch, "STOCK_RESERVATION_RELEASE", 0, null, 
                "Released reservation back to available stock (" + quantity + " units)");    
    }

    @Override
    @Transactional
    public PartResponseDTO addStockWithConversion(Long id, int inputQty, String inputUom,
                                                   String batch, String loc) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        double factor = 1.0;
        if (!part.getUom().equals(inputUom)) {
            UomConversion conv = uomRepository.findByFromUomAndToUom(inputUom, part.getUom())
                    .orElseThrow(() -> new RuntimeException("No UOM conversion found: "
                            + inputUom + " -> " + part.getUom()));
            factor = conv.getFactor();
        }

        return addStock(id, (int) (inputQty * factor), batch, loc, null,
                null, null, null, null, null, null);
    }

    @Override
    @Transactional
    public void transferStock(Long id, String batch, String fromBin, String toBin, int qty) {
        if(qty <= 0) throw new IllegalArgumentException("Transfer quantity must be greater than zero");
        if(fromBin != null && fromBin.equals(toBin)) {
            throw new IllegalArgumentException("Source and target bin locations must be different");
        }
        locationService.assertActive(toBin);

        StockBalance source = balanceRepository
                .findByPartIdAndBatchNumberAndLocation(id, batch, fromBin)
                .orElseThrow(() -> new RuntimeException("Source bin location not found"));

        if (source.getQuantity() < qty) throw new RuntimeException("Not enough stock in source bin");

        source.setQuantity(source.getQuantity() - qty);
        if(source.getQuantity() == 0) {
            balanceRepository.delete(source);
        } else {
            balanceRepository.save(source);
        }

        StockBalance target = balanceRepository
                .findByPartIdAndBatchNumberAndLocation(id, batch, toBin)
                .orElse(StockBalance.builder()
                        .partId(id).batchNumber(batch)
                        .location(toBin).status(source.getStatus()).quantity(0).build());

        target.setQuantity(target.getQuantity() + qty);
        target.setExpiryDate(source.getExpiryDate());
        balanceRepository.save(target);

        // Net quantity for this batch is unchanged — same bin-to-bin caveat as
        // STOCK_RESERVATION above. Log 0 so the ledger's running balance stays accurate.
        transactionService.logTransaction(id, batch, "LOCATION_TRANSFER",
                0, null, "Moved " + qty + " units from " + fromBin + " to " + toBin);
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    // Total APPROVED (available) stock across every batch/location for this part —
    // the number reorder-level comparisons must use, not any single batch/location row.
    private int totalApprovedStock(Long partId) {
        return balanceRepository.findAllBalancesByPart(partId).stream()
                .filter(b -> b.getStatus() == StockStatus.APPROVED)
                .mapToInt(b -> b.getQuantity() != null ? b.getQuantity() : 0)
                .sum();
    }

    // Centralized low-stock check — must be called after ANY change that can reduce a
    // part's total APPROVED quantity (material issue, QA rejection, reservation), not just
    // the original "issue material to a Work Order" path. Missing this on a path means a
    // part can silently drop below reorder level with no auto-PR ever raised for it.
    private void checkAndPublishLowStock(Part part, int totalRemainingApproved, String batchNumber, String location) {
        if (part.getReorderLevel() != null && totalRemainingApproved <= part.getReorderLevel()) {
            try {
                inventoryProducer.sendLowStockEvent(
                    StockLowEvent.builder()
                        .partId(part.getId())
                        .partName(part.getName())
                        .skuCode(part.getSkuCode())
                        .batchNumber(batchNumber != null ? batchNumber : "")
                        .currentQuantity(totalRemainingApproved)
                        .reorderLevel(part.getReorderLevel())
                        .location(location != null ? location : "")
                        .build()
                );
            } catch (Exception ex) {
                log.warn("[KAFKA] STOCK_LOW publish failed for partId={}: {}", part.getId(), ex.getMessage());
            }
        }
    }

    /** Builds a structured GRN remark string for transaction traceability (F-36 GRN Form) */
    private String buildGrnRemark(String grnNumber, String challanNumber,
                                   String supplierName, String transportName,
                                   int receivedQty, Integer inspectionQty, StockStatus targetStatus) {
        StringBuilder sb = new StringBuilder("Inward to ").append(targetStatus.name());
        if (grnNumber    != null && !grnNumber.isBlank())    sb.append(" | GRN:").append(grnNumber.trim());
        if (challanNumber!= null && !challanNumber.isBlank()) sb.append(" | Challan:").append(challanNumber.trim());
        if (supplierName != null && !supplierName.isBlank())  sb.append(" | Supplier:").append(supplierName.trim());
        if (transportName!= null && !transportName.isBlank()) sb.append(" | Transport:").append(transportName.trim());
        sb.append(" | Recd Qty:").append(receivedQty);
        if (inspectionQty != null)                           sb.append(" | Insp Qty:").append(inspectionQty);
        return sb.toString();
    }

    /**
     * ITM-BR-002: batch and serial tracking are mutually exclusive.
     * ITM-BR-003: expiry tracking requires a shelf life.
     * ITM-BR-006: minStock <= reorderLevel <= maxStock when all three are set.
     */
    private void validateItemMasterRules(boolean batchRequired, boolean serialRequired,
                                          boolean expiryTracking, Integer shelfLifeDays,
                                          Integer minStock, Integer reorderLevel, Integer maxStock) {
        if (batchRequired && serialRequired) {
            throw new IllegalArgumentException(
                    "ITM-BR-002: Batch and Serial tracking are mutually exclusive.");
        }
        if (expiryTracking && shelfLifeDays == null) {
            throw new IllegalArgumentException(
                    "ITM-BR-003: Expiry tracking requires a shelf life (days) to be set.");
        }
        if (minStock != null && reorderLevel != null && minStock > reorderLevel) {
            throw new IllegalArgumentException(
                    "ITM-BR-006: Invalid limits — Min Stock must be <= Reorder Level.");
        }
        if (reorderLevel != null && maxStock != null && reorderLevel > maxStock) {
            throw new IllegalArgumentException(
                    "ITM-BR-006: Invalid limits — Reorder Level must be <= Max Stock.");
        }
    }

    private PartResponseDTO mapWithLiveStock(Part part, StockStatus status) {
        PartResponseDTO dto = mapper.toDTO(part);

        List<StockBalance> balances = balanceRepository.findAllBalancesByPart(part.getId());

        // Authoritative per-batch breakdown — a part can be APPROVED and PENDING_QA
        // (and RESERVED, and REJECTED) at once, across different batches. Consumers
        // that care about a specific bucket (e.g. the QA queue) must read this list;
        // the top-level fields below are only ever an APPROVED-stock summary and
        // cannot represent more than one status at a time.
        dto.setBalances(balances.stream()
                .map(b -> StockBalanceDTO.builder()
                        .batchNumber(b.getBatchNumber())
                        .status(b.getStatus().name())
                        .quantity(b.getQuantity())
                        .location(b.getLocation())
                        .expiryDate(b.getExpiryDate())
                        .poNumber(b.getPoNumber())
                        .build())
                .toList());

        // Surfaced separately from the APPROVED-only summary above so callers (e.g.
        // the Work Order Stock Check widget) can tell "arrived but awaiting QA"
        // apart from "not arrived at all" instead of both looking like a shortage.
        long pendingQaQty = balances.stream()
                .filter(b -> b.getStatus() == StockStatus.PENDING_QA)
                .mapToLong(b -> b.getQuantity() != null ? b.getQuantity() : 0L)
                .sum();
        dto.setPendingQaQuantity((int) pendingQaQty);

        if (status != null) {
            // Caller asked for a specific bucket (e.g. getById(..., APPROVED)) — keep
            // that exact, pre-existing behavior.
            long qty = balances.stream()
                    .filter(b -> b.getStatus() == status)
                    .mapToLong(b -> b.getQuantity() != null ? b.getQuantity() : 0L)
                    .sum();
            dto.setQuantity((int) qty);
            balances.stream()
                    .filter(b -> b.getStatus() == status)
                    .findFirst()
                    .ifPresentOrElse(b -> {
                        dto.setBatchNumber(b.getBatchNumber());
                        dto.setStatus(b.getStatus().name());
                        dto.setLocation(b.getLocation());
                        dto.setExpiryDate(b.getExpiryDate());
                    }, () -> dto.setStatus("EMPTY"));
            return dto;
        }

        // General list/search/get: the top-level fields summarize USABLE (APPROVED)
        // stock only — pending/rejected/reserved batches must never be counted as
        // available or silently mask each other out here.
        long approvedQty = balances.stream()
                .filter(b -> b.getStatus() == StockStatus.APPROVED)
                .mapToLong(b -> b.getQuantity() != null ? b.getQuantity() : 0L)
                .sum();
        dto.setQuantity((int) approvedQty);

        StockBalance approvedBalance = balances.stream()
                .filter(b -> b.getStatus() == StockStatus.APPROVED)
                .findFirst()
                .orElse(null);

        if (approvedBalance != null) {
            dto.setBatchNumber(approvedBalance.getBatchNumber());
            dto.setStatus(StockStatus.APPROVED.name());
            dto.setLocation(approvedBalance.getLocation());
            dto.setExpiryDate(approvedBalance.getExpiryDate());
        } else if (!balances.isEmpty()) {
            // No usable stock, but something exists (pending/reserved/rejected) —
            // surface the most attention-worthy one instead of claiming EMPTY.
            StockBalance attentionBalance = balances.stream()
                    .min(java.util.Comparator.comparingInt(b -> switch (b.getStatus()) {
                        case PENDING_QA -> 0;
                        case REJECTED   -> 1;
                        case RESERVED   -> 2;
                        case APPROVED   -> 3; // unreachable here, kept for exhaustiveness
                    }))
                    .orElse(balances.get(0));
            dto.setBatchNumber(attentionBalance.getBatchNumber());
            dto.setStatus(attentionBalance.getStatus().name());
            dto.setLocation(attentionBalance.getLocation());
            dto.setExpiryDate(attentionBalance.getExpiryDate());
        } else {
            dto.setStatus("EMPTY");
        }

        return dto;
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ImportResultDTO importParts(List<PartImportRowDTO> rows) {
        List<ImportRowError> errors = new java.util.ArrayList<>();
        int success = 0;

        for (int i = 0; i < rows.size(); i++) {
            PartImportRowDTO row = rows.get(i);
            int rowNumber = i + 1;
            try {
                if (row.getName() == null || row.getName().isBlank()) {
                    throw new IllegalArgumentException("Name is required");
                }
                if (row.getSkuCode() == null || row.getSkuCode().isBlank()) {
                    throw new IllegalArgumentException("SKU Code is required");
                }
                if (partRepository.findBySkuCode(row.getSkuCode()).isPresent()) {
                    throw new IllegalArgumentException("SKU already exists: " + row.getSkuCode());
                }

                Part part = Part.builder()
                        .name(row.getName())
                        .skuCode(row.getSkuCode())
                        .partType(row.getPartType())
                        .uom(row.getUom())
                        .reorderLevel(row.getReorderLevel())
                        .criticality(row.getCriticality())
                        .build();
                partRepository.save(part);
                success++;
            } catch (Exception e) {
                errors.add(new ImportRowError(rowNumber, row.getSkuCode(), e.getMessage()));
            }
        }

        return ImportResultDTO.builder()
                .successCount(success)
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }
}