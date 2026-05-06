package com.aerospace.inventory_service.service.impl;

import com.aerospace.inventory_service.dto.*;
import com.aerospace.inventory_service.entity.*;
import com.aerospace.inventory_service.event.*;
import com.aerospace.inventory_service.event.producer.InventoryProducer;
import com.aerospace.inventory_service.exception.ResourceNotFoundException;
import com.aerospace.inventory_service.mapper.PartMapper;
import com.aerospace.inventory_service.repository.*;
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
import java.util.List;

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

    // ── CREATE PART ──────────────────────────────────────────────────────────
    // Kafka: PART_CREATED → notification-service logs "New Part Registered"

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PartResponseDTO createPart(PartRequestDTO dto) {
        if (partRepository.findBySkuCode(dto.getSerialNumber()).isPresent()) {
            throw new RuntimeException("Part with this SKU/Serial already exists");
        }
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

    // ── ADD STOCK (inward-from-grn) ──────────────────────────────────────────
    // Kafka: STOCK_RECEIVED → notification-service logs "Stock Received"

    @Override
    @Transactional
    public PartResponseDTO addStock(Long id, int quantity, String batch, String loc, LocalDateTime expiryDate) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        StockBalance balance = balanceRepository
                .findByPartIdAndBatchNumberAndStatus(id, batch, StockStatus.PENDING_QA)
                .orElse(StockBalance.builder()
                        .partId(id).batchNumber(batch)
                        .status(StockStatus.PENDING_QA).quantity(0).build());

        balance.setQuantity(balance.getQuantity() + quantity);
        balance.setLocation(loc);
        balance.setExpiryDate(expiryDate);
        balanceRepository.save(balance);

        transactionService.logTransaction(id, batch, "GRN_RECEIPT", quantity, "Inward to PENDING_QA");

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

        PartResponseDTO response = mapper.toDTO(part);
        response.setQuantity(balance.getQuantity());
        response.setBatchNumber(batch);
        response.setLocation(loc);
        response.setExpiryDate(expiryDate);
        response.setStatus(StockStatus.PENDING_QA.name());
        return response;
    }

    // ── ISSUE TO PRODUCTION ──────────────────────────────────────────────────
    // Kafka: MATERIAL_ISSUED + (optionally) STOCK_LOW

    @Override
    @Transactional
    public PartResponseDTO issueToProduction(Long id, int quantity, String batch) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        StockBalance balance = balanceRepository
                .findByPartIdAndBatchNumberAndStatus(id, batch, StockStatus.APPROVED)
                .orElseThrow(() -> new RuntimeException("No APPROVED stock for batch: " + batch));

        if (balance.getQuantity() < quantity) throw new RuntimeException("Insufficient approved stock");

        balance.setQuantity(balance.getQuantity() - quantity);
        balanceRepository.save(balance);

        transactionService.logTransaction(id, batch, "PRODUCTION_ISSUE", -quantity, "Material Issued");

        int remaining = balance.getQuantity();

        try {
            inventoryProducer.sendMaterialIssuedEvent(
                MaterialIssuedEvent.builder()
                    .partId(id)
                    .partName(part.getName())
                    .batchNumber(batch)
                    .quantityIssued(quantity)
                    .remainingQuantity(remaining)
                    .issuedTo("Production")
                    .issuedBy("System")
                    .location(balance.getLocation() != null ? balance.getLocation() : "")
                    .build()
            );
        } catch (Exception ex) {
            log.warn("[KAFKA] MATERIAL_ISSUED publish failed for partId={}: {}", id, ex.getMessage());
        }

        // Trigger LOW_STOCK alert if remaining drops at or below reorder level
        if (part.getReorderLevel() != null && remaining <= part.getReorderLevel()) {
            try {
                inventoryProducer.sendLowStockEvent(
                    StockLowEvent.builder()
                        .partId(id)
                        .partName(part.getName())
                        .batchNumber(batch)
                        .currentQuantity(remaining)
                        .reorderLevel(part.getReorderLevel())
                        .location(balance.getLocation() != null ? balance.getLocation() : "")
                        .build()
                );
            } catch (Exception ex) {
                log.warn("[KAFKA] STOCK_LOW publish failed for partId={}: {}", id, ex.getMessage());
            }
        }

        PartResponseDTO response = mapper.toDTO(part);
        response.setQuantity(remaining);
        response.setBatchNumber(batch);
        response.setStatus(StockStatus.APPROVED.name());
        return response;
    }

    // ── PROCESS QA DECISION ──────────────────────────────────────────────────
    // Kafka: QA_STATUS_CHANGED → notification-service logs QA_APPROVED / QA_REJECTED

    @Override
    @Transactional
    public PartResponseDTO processQADecision(Long id, String batch, String newStatus, String remarks) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        StockBalance pending = balanceRepository
                .findByPartIdAndBatchNumberAndStatus(id, batch, StockStatus.PENDING_QA)
                .orElseThrow(() -> new ResourceNotFoundException("Pending stock not found for batch: " + batch));

        int qty = pending.getQuantity();
        String loc = pending.getLocation();
        LocalDateTime expiry = pending.getExpiryDate();
        balanceRepository.delete(pending);

        StockBalance target = balanceRepository
                .findByPartIdAndBatchNumberAndStatus(id, batch, StockStatus.valueOf(newStatus))
                .orElse(StockBalance.builder()
                        .partId(id).batchNumber(batch)
                        .status(StockStatus.valueOf(newStatus)).quantity(0).build());

        target.setQuantity(target.getQuantity() + qty);
        target.setLocation(loc);
        target.setExpiryDate(expiry);
        balanceRepository.save(target);

        transactionService.logTransaction(id, batch, "QA_DECISION", 0,
                "Moved to " + newStatus + ": " + remarks);

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
                    .build()
            );
        } catch (Exception ex) {
            log.warn("[KAFKA] QA_STATUS_CHANGED publish failed for partId={}: {}", id, ex.getMessage());
        }

        PartResponseDTO response = mapper.toDTO(part);
        response.setQuantity(target.getQuantity());
        response.setBatchNumber(batch);
        response.setStatus(newStatus);
        response.setQaRemarks(remarks);
        response.setExpiryDate(expiry);
        return response;
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
        partRepository.deleteById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PartResponseDTO updatePart(Long id, PartRequestDTO dto) {
        log.info("ERP: Updating material master for id: {}", id);
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found"));

        part.setName(dto.getName());
        part.setPartType(dto.getPartType());
        part.setCriticality(dto.getCriticality());
        part.setReorderLevel(dto.getReorderLevel());
        part.setSkuCode(dto.getSerialNumber());

        return mapWithLiveStock(partRepository.save(part), null);
    }

    @Override
    @Transactional
    public void reserveStock(Long partId, String batch, int quantity) {
        StockBalance approvedStock = balanceRepository
                .findByPartIdAndBatchNumberAndStatus(partId, batch, StockStatus.APPROVED)
                .orElseThrow(() -> new RuntimeException("Approved stock not found"));

        if (approvedStock.getQuantity() < quantity)
            throw new RuntimeException("Insufficient stock to reserve");

        approvedStock.setQuantity(approvedStock.getQuantity() - quantity);
        balanceRepository.save(approvedStock);

        StockBalance reservedStock = balanceRepository
                .findByPartIdAndBatchNumberAndStatus(partId, batch, StockStatus.RESERVED)
                .orElse(StockBalance.builder()
                        .partId(partId).batchNumber(batch)
                        .status(StockStatus.RESERVED).quantity(0).build());

        reservedStock.setExpiryDate(approvedStock.getExpiryDate());
        reservedStock.setQuantity(reservedStock.getQuantity() + quantity);
        balanceRepository.save(reservedStock);

        transactionService.logTransaction(partId, batch, "STOCK_RESERVATION",
                quantity, "Reserved for Work Order");
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

        return addStock(id, (int) (inputQty * factor), batch, loc, null);
    }

    @Override
    @Transactional
    public void transferStock(Long id, String batch, String fromBin, String toBin, int qty) {
        StockBalance source = balanceRepository
                .findByPartIdAndBatchNumberAndLocation(id, batch, fromBin)
                .orElseThrow(() -> new RuntimeException("Source bin location not found"));

        if (source.getQuantity() < qty) throw new RuntimeException("Not enough stock in source bin");

        source.setQuantity(source.getQuantity() - qty);
        balanceRepository.save(source);

        StockBalance target = balanceRepository
                .findByPartIdAndBatchNumberAndLocation(id, batch, toBin)
                .orElse(StockBalance.builder()
                        .partId(id).batchNumber(batch)
                        .location(toBin).status(source.getStatus()).quantity(0).build());

        target.setQuantity(target.getQuantity() + qty);
        target.setExpiryDate(source.getExpiryDate());
        balanceRepository.save(target);

        transactionService.logTransaction(id, batch, "LOCATION_TRANSFER",
                qty, "Moved from " + fromBin + " to " + toBin);
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private PartResponseDTO mapWithLiveStock(Part part, StockStatus status) {
        PartResponseDTO dto = mapper.toDTO(part);

        List<StockBalance> balances = balanceRepository.findAllBalancesByPart(part.getId());

        long totalQty = balances.stream()
                .filter(b -> status == null || b.getStatus() == status)
                .mapToLong(b -> b.getQuantity() != null ? b.getQuantity() : 0L)
                .sum();
        dto.setQuantity((int) totalQty);

        if (!balances.isEmpty()) {
            StockBalance primaryBalance = balances.stream()
                    .min(java.util.Comparator.comparingInt(b -> switch (b.getStatus()) {
                        case APPROVED   -> 0;
                        case PENDING_QA -> 1;
                        case RESERVED   -> 2;
                        case REJECTED   -> 3;
                    }))
                    .orElse(balances.get(0));

            dto.setBatchNumber(primaryBalance.getBatchNumber());
            dto.setStatus(primaryBalance.getStatus().name());
            dto.setLocation(primaryBalance.getLocation());
            dto.setExpiryDate(primaryBalance.getExpiryDate());
        } else {
            dto.setStatus("EMPTY");
        }

        return dto;
    }
}