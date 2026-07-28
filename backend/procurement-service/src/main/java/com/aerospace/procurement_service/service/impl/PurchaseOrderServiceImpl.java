package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.POApprovalActionRequest;
import com.aerospace.procurement_service.dto.POApprovalHistoryDTO;
import com.aerospace.procurement_service.dto.POLineItemDTO;
import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;
import com.aerospace.procurement_service.entity.ApprovalActionStatus;
import com.aerospace.procurement_service.entity.ApprovalMatrix;
import com.aerospace.procurement_service.entity.POApprovalHistory;
import com.aerospace.procurement_service.entity.POLineItem;
import com.aerospace.procurement_service.entity.POStatus;
import com.aerospace.procurement_service.entity.PurchaseOrder;
import com.aerospace.procurement_service.entity.PurchaseRequisition;
import com.aerospace.procurement_service.entity.Quotation;
import com.aerospace.procurement_service.entity.RequestForQuotation;
import com.aerospace.procurement_service.entity.Supplier;
import com.aerospace.procurement_service.exception.ResourceNotFoundException;
import com.aerospace.procurement_service.mapper.PurchaseOrderMapper;
import com.aerospace.procurement_service.repository.POApprovalHistoryRepository;
import com.aerospace.procurement_service.repository.PurchaseOrderRepository;
import com.aerospace.procurement_service.service.ApprovalMatrixService;
import com.aerospace.procurement_service.service.DocumentSequenceService;
import com.aerospace.procurement_service.service.PurchaseOrderService;
import com.aerospace.procurement_service.service.SupplierService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final String MODULE = "PURCHASE_ORDER";
    // PUR-BR-005: same supplier + at least one shared item, still open, within this window
    private static final int DUPLICATE_WINDOW_DAYS = 3;

    private final PurchaseOrderRepository poRepository;
    private final SupplierService supplierService;
    private final PurchaseOrderMapper poMapper;
    private final ApprovalMatrixService approvalMatrixService;
    private final POApprovalHistoryRepository approvalHistoryRepository;
    private final DocumentSequenceService documentSequenceService;
    private final com.aerospace.procurement_service.event.producer.ProcurementEventPublisher eventPublisher;
    private final com.aerospace.procurement_service.repository.PurchaseRequisitionRepository purchaseRequisitionRepository;

    @org.springframework.beans.factory.annotation.Value("${procurement.po.require-pr-origin:true}")
    private boolean requirePrOrigin;


    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        // PUR-BR-002-ORIGIN: every PO must trace back to an approved PR, unless the
        // tenant has explicitly configured this rule off.
        if (request.getPurchaseRequisitionId() == null) {
            if (requirePrOrigin) {
                throw new IllegalStateException(
                        "PUR-BR-002: Purchase Orders must originate from an approved Purchase Requisition. " +
                                "Set procurement.po.require-pr-origin=false to allow direct POs.");
            }
        } else {
            com.aerospace.procurement_service.entity.PurchaseRequisition sourcePr = purchaseRequisitionRepository
                    .findById(request.getPurchaseRequisitionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Purchase Requisition not found: " + request.getPurchaseRequisitionId()));
            if (sourcePr.getStatus() == com.aerospace.procurement_service.entity.PRStatus.CANCELLED) {
                throw new IllegalStateException(
                        "Cannot raise a PO from cancelled Purchase Requisition " + sourcePr.getPrNumber() + ".");
            }
        }

        // 1. Fetch the supplier. (If not found, it throws our custom exception)
        Supplier supplier = supplierService.getSupplierById(request.getSupplierId());

        // Aerospace Business Rule: Is the supplier certified?
        if (!"CERTIFIED".equalsIgnoreCase(supplier.getCertificationStatus())) {
            throw new IllegalStateException("Cannot create PO for an uncertified supplier!");
        }
        if (!supplier.isActive()) {
            throw new IllegalStateException("Cannot create PO for a deactivated supplier!");
        }

        // 2. Map the request to an Entity
        PurchaseOrder po = poMapper.toEntity(request);

        // 3. Set server-controlled data (Things the user shouldn't send)
        po.setSupplier(supplier);
        po.setOrderDate(LocalDateTime.now());
        po.setStatus(POStatus.DRAFT);
                // ARC-BR-006: PO numbers are always system-generated via the document sequence, never ad hoc
        po.setPoNumber(documentSequenceService.generate("PO"));
        po.setSourcePurchaseRequisitionId(request.getPurchaseRequisitionId());

        // 4. Calculate total amount by looping through line items
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (po.getItems() != null) {
            for (POLineItem item : po.getItems()) {
                // The POLineItem entity has a @PrePersist method to calculate its own total,
                // but we need the sum of all items for the main PO document.
                BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setTotalPrice(lineTotal); // Ensure it's set
                totalAmount = totalAmount.add(lineTotal);
            }
        }
        po.setTotalAmount(totalAmount);

        // PUR-BR-005: non-blocking duplicate-PO warning
        String duplicateWarning = findDuplicateWarning(supplier.getId(), po);

        // 5. Save to database
        PurchaseOrder savedPo = poRepository.save(po);

        // 6. Convert back to a response DTO for the frontend
        PurchaseOrderResponse response = poMapper.toDTO(savedPo);
        response.setDuplicateWarning(duplicateWarning);
        return response;
    }

    private String findDuplicateWarning(Long supplierId, PurchaseOrder newPo) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(DUPLICATE_WINDOW_DAYS);
        List<String> newSkus = newPo.getItems() == null ? List.of() :
                newPo.getItems().stream().map(POLineItem::getSkuCode).toList();

        boolean duplicateExists = poRepository.findAll().stream()
                .filter(existing -> existing.getSupplier() != null && existing.getSupplier().getId().equals(supplierId))
                .filter(existing -> existing.getStatus() != POStatus.CANCELLED && existing.getStatus() != POStatus.CLOSED)
                .filter(existing -> existing.getOrderDate() != null && existing.getOrderDate().isAfter(cutoff))
                .anyMatch(existing -> existing.getItems() != null && existing.getItems().stream()
                        .anyMatch(line -> newSkus.contains(line.getSkuCode())));

        return duplicateExists
                ? "PUR-BR-005: Warning: an open PO already exists for this supplier with an overlapping item within the last "
                        + DUPLICATE_WINDOW_DAYS + " days."
                : null;
    }

    @Override
    public java.util.List<PurchaseOrderResponse> getAllPurchaseOrders() {
        return poRepository.findAll().stream()
                .map(poMapper::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    @Override
    public PurchaseOrderResponse updatePOStatus(Long id, POStatus status) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PO not found with id " + id));

        // The approval chain has its own dedicated endpoints — this generic endpoint must not
        // be used to bypass PUR-BR-001/PUR-BR-002.
        if (status == POStatus.DRAFT || status == POStatus.PENDING_APPROVAL
                || status == POStatus.APPROVED || status == POStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "Use /submit, /approve or /reject to move a PO into " + status + " — not this endpoint.");
        }
        if (status == POStatus.SENT_TO_VENDOR && po.getStatus() != POStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "PO must be APPROVED before it can be sent to the vendor (current: " + po.getStatus() + ").");
        }

        po.setStatus(status);
        PurchaseOrder saved = poRepository.save(po);

        if (status == POStatus.SENT_TO_VENDOR) {
            com.aerospace.procurement_service.event.PurchaseOrderCreatedEvent event = com.aerospace.procurement_service.event.PurchaseOrderCreatedEvent.builder()
                    .purchaseOrderId(saved.getId())
                    .poNumber(saved.getPoNumber())
                    .supplierName(saved.getSupplier().getName())
                    .totalAmount(saved.getTotalAmount() != null ? saved.getTotalAmount().doubleValue() : 0.0)
                    .currency(saved.getCurrency())
                    .expectedDeliveryDate(saved.getDeliveryDate())
                    .build();
            eventPublisher.publishPOCreatedEvent(event);
        }

        return poMapper.toDTO(saved);
    }

    @Transactional
    @Override
    public PurchaseOrderResponse submitForApproval(Long id) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PO not found with id " + id));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new IllegalArgumentException("Only a DRAFT PO can be submitted for approval (current: " + po.getStatus() + ").");
        }

        // Only bump the version on a RESUBMISSION (i.e. a prior chain already exists for this PO —
        // it was rejected). The very first submission keeps the PO's initial documentVersion (1).
        boolean hasPriorSubmission = !approvalHistoryRepository
                .findByPurchaseOrder_IdOrderByDocumentVersionDescApprovalLevelAsc(id).isEmpty();
        if (hasPriorSubmission) {
            po.setDocumentVersion(po.getDocumentVersion() + 1);
        }
        int version = po.getDocumentVersion();

        List<ApprovalMatrix> bands = approvalMatrixService.evaluateChain(MODULE, po.getTotalAmount());

        if (bands.isEmpty()) {
            // Doc step 2: no qualifying band → auto-approve, no chain needed
            po.setStatus(POStatus.APPROVED);
            po.setCurrentApprovalLevel(null);
            return poMapper.toDTO(poRepository.save(po));
        }

        for (ApprovalMatrix band : bands) {
            approvalHistoryRepository.save(POApprovalHistory.builder()
                    .purchaseOrder(po)
                    .approvalLevel(band.getLevelSequence())
                    .approverRole(band.getApproverRole())
                    .status(band.getLevelSequence() == bands.get(0).getLevelSequence()
                            ? ApprovalActionStatus.PENDING : ApprovalActionStatus.WAITING)
                    .documentVersion(version)
                    .build());
        }

        po.setStatus(POStatus.PENDING_APPROVAL);
        po.setCurrentApprovalLevel(bands.get(0).getLevelSequence());

        return poMapper.toDTO(poRepository.save(po));
    }

    @Transactional
    @Override
    public PurchaseOrderResponse approve(Long id, POApprovalActionRequest request, String approverUsername, List<String> approverRoles) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PO not found with id " + id));

        if (po.getStatus() != POStatus.PENDING_APPROVAL || po.getCurrentApprovalLevel() == null) {
            throw new IllegalArgumentException("PO is not awaiting approval (current: " + po.getStatus() + ").");
        }

        POApprovalHistory pending = approvalHistoryRepository
                .findByPurchaseOrder_IdAndDocumentVersionAndApprovalLevel(id, po.getDocumentVersion(), po.getCurrentApprovalLevel())
                .orElseThrow(() -> new IllegalStateException("No pending approval row found for level " + po.getCurrentApprovalLevel()));

        if (!approverRoles.contains(pending.getApproverRole())) {
            throw new IllegalArgumentException(
                    "PUR-BR-001: This PO requires role " + pending.getApproverRole() + " to approve at level " + pending.getApprovalLevel() + ".");
        }

        LocalDateTime now = LocalDateTime.now();
        pending.setStatus(ApprovalActionStatus.APPROVED);
        pending.setApproverUsername(approverUsername);
        pending.setApprovedAt(now);
        pending.setRemarks(request.getRemarks());
        pending.setDigitalSignatureHash(computeSignatureHash(approverUsername, now, id, po.getDocumentVersion(), request.getRemarks()));
        approvalHistoryRepository.save(pending);

        List<POApprovalHistory> chain = approvalHistoryRepository
                .findByPurchaseOrder_IdAndDocumentVersion(id, po.getDocumentVersion());
        int maxLevel = chain.stream().mapToInt(POApprovalHistory::getApprovalLevel).max().orElse(pending.getApprovalLevel());

        if (pending.getApprovalLevel() >= maxLevel) {
            // Last qualifying level cleared → fully approved
            po.setStatus(POStatus.APPROVED);
            po.setCurrentApprovalLevel(null);
        } else {
            int nextLevel = chain.stream()
                    .mapToInt(POApprovalHistory::getApprovalLevel)
                    .filter(lvl -> lvl > pending.getApprovalLevel())
                    .min().orElseThrow();
            POApprovalHistory nextRow = chain.stream()
                    .filter(h -> h.getApprovalLevel() == nextLevel)
                    .findFirst().orElseThrow();
            nextRow.setStatus(ApprovalActionStatus.PENDING);
            approvalHistoryRepository.save(nextRow);
            po.setCurrentApprovalLevel(nextLevel);
        }

        return poMapper.toDTO(poRepository.save(po));
    }

    @Transactional
    @Override
    public PurchaseOrderResponse reject(Long id, POApprovalActionRequest request, String approverUsername, List<String> approverRoles) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PO not found with id " + id));

        if (po.getStatus() != POStatus.PENDING_APPROVAL || po.getCurrentApprovalLevel() == null) {
            throw new IllegalArgumentException("PO is not awaiting approval (current: " + po.getStatus() + ").");
        }
        if (request.getRemarks() == null || request.getRemarks().isBlank()) {
            throw new IllegalArgumentException("PUR-BR-002: Rejection remarks are mandatory.");
        }

        POApprovalHistory pending = approvalHistoryRepository
                .findByPurchaseOrder_IdAndDocumentVersionAndApprovalLevel(id, po.getDocumentVersion(), po.getCurrentApprovalLevel())
                .orElseThrow(() -> new IllegalStateException("No pending approval row found for level " + po.getCurrentApprovalLevel()));

        if (!approverRoles.contains(pending.getApproverRole())) {
            throw new IllegalArgumentException(
                    "PUR-BR-001: This PO requires role " + pending.getApproverRole() + " to reject at level " + pending.getApprovalLevel() + ".");
        }

        LocalDateTime now = LocalDateTime.now();
        pending.setStatus(ApprovalActionStatus.REJECTED);
        pending.setApproverUsername(approverUsername);
        pending.setApprovedAt(now);
        pending.setRemarks(request.getRemarks());
        pending.setDigitalSignatureHash(computeSignatureHash(approverUsername, now, id, po.getDocumentVersion(), request.getRemarks()));
        approvalHistoryRepository.save(pending);

        // PUR-BR-002/PUR-BR-003: rejection resets the PO to Draft; a full resubmission is required
        po.setStatus(POStatus.DRAFT);
        po.setCurrentApprovalLevel(null);

        return poMapper.toDTO(poRepository.save(po));
    }

    @Transactional
    @Override
    public PurchaseOrderResponse updatePurchaseOrder(Long id, PurchaseOrderRequest request) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PO not found with id " + id));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new IllegalArgumentException("Only a DRAFT PO can be edited (current: " + po.getStatus() + "). Reject it back to Draft first if needed.");
        }

        Supplier supplier = supplierService.getSupplierById(request.getSupplierId());
        if (!"CERTIFIED".equalsIgnoreCase(supplier.getCertificationStatus())) {
            throw new IllegalStateException("Cannot assign an uncertified supplier to this PO!");
        }
        if (!supplier.isActive()) {
            throw new IllegalStateException("Cannot assign a deactivated supplier to this PO!");
        }

        po.setSupplier(supplier);
        po.setDeliveryDate(request.getDeliveryDate());
        po.setCurrency(request.getCurrency());
        po.setRemarks(request.getRemarks());
        po.setShipToName(request.getShipToName());
        po.setShipToAddress(request.getShipToAddress());
        po.setShipToState(request.getShipToState());
        po.setShipToStateCode(request.getShipToStateCode());
        po.setPaymentTerms(request.getPaymentTerms());
        po.setPayment(request.getPayment());
        po.setDeliveryTerms(request.getDeliveryTerms());
        po.setExciseDuty(request.getExciseDuty());
        po.setTermsAndConditions(request.getTermsAndConditions());

        // Replace line items wholesale — orphanRemoval on PurchaseOrder.items deletes the old rows
        po.getItems().clear();
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (POLineItemDTO dto : request.getItems()) {
                POLineItem item = poMapper.toLineItemEntity(dto, po);
                BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setTotalPrice(lineTotal);
                po.getItems().add(item);
                totalAmount = totalAmount.add(lineTotal);
            }
        }
        po.setTotalAmount(totalAmount);

        return poMapper.toDTO(poRepository.save(po));
    }

    @Transactional
    @Override
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PO not found with id " + id));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new IllegalArgumentException("Only a DRAFT PO can be deleted (current: " + po.getStatus() + ").");
        }
        boolean everSubmitted = !approvalHistoryRepository
                .findByPurchaseOrder_IdOrderByDocumentVersionDescApprovalLevelAsc(id).isEmpty();
        if (everSubmitted) {
            throw new IllegalArgumentException(
                    "This PO has approval history and can't be deleted outright — it's preserved for audit. Leave it in Draft or edit it instead.");
        }
        poRepository.delete(po);
    }

    @Override
    public List<POApprovalHistoryDTO> getApprovalHistory(Long id) {
        return approvalHistoryRepository.findByPurchaseOrder_IdOrderByDocumentVersionDescApprovalLevelAsc(id).stream()
                .map(poMapper::toApprovalHistoryDTO)
                .collect(Collectors.toList());
    }

    private String computeSignatureHash(String approverUsername, LocalDateTime timestamp, Long poId, int documentVersion, String remarks) {
        String payload = approverUsername + "|" + timestamp + "|" + poId + "|" + documentVersion + "|" + (remarks != null ? remarks : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every standard JVM
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Transactional
    @Override
    public void createDraftPOFromStockLow(com.aerospace.procurement_service.event.StockLowEvent event) {
        Supplier defaultSupplier = firstCertifiedSupplier();
        if (defaultSupplier == null) {
            log.warn("Cannot create draft PO automatically because no certified suppliers exist!");
            return;
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(defaultSupplier);
        po.setOrderDate(LocalDateTime.now());
        po.setDeliveryDate(LocalDateTime.now().plusDays(7));
        po.setStatus(POStatus.DRAFT);
        po.setPoNumber(documentSequenceService.generate("PO"));
        po.setCurrency("USD");
        po.setRemarks("AUTO-RAISED: low stock alert for " + event.getPartName()
                + " — verify price before submitting.");

        POLineItem item = new POLineItem();
        item.setPurchaseOrder(po);
        // The real SKU, not the display name — a wrong code here fails GRN receipt later.
        item.setSkuCode(event.getSkuCode() != null ? event.getSkuCode() : event.getPartName());
        item.setQuantity(event.getReorderLevel() * 2);
        item.setUnitPrice(BigDecimal.ZERO); // unknown at auto-raise time — officer fills the real price
        item.setTotalPrice(BigDecimal.ZERO);
        item.setUom("PCS");

        po.setItems(java.util.List.of(item));
        po.setTotalAmount(item.getTotalPrice());

        poRepository.save(po);
        log.info("📢 [PROCUREMENT AUTO] Successfully created draft PO: {} for Part: {}", po.getPoNumber(), event.getPartName());
    }

    // ── AUTO-RAISE PO FOR WORK-ORDER SHORTAGE ────────────────────────────────
    // Kafka-driven, like createDraftPOFromStockLow above — this repo has no
    // cross-service REST calls anywhere, so the "which supplier" decision either
    // comes from the part's preferredSupplierId (carried on the event) or falls
    // back to the same first-certified-supplier heuristic already used for
    // low-stock auto-drafting.
    @Transactional
    @Override
    public void createDraftPOFromShortage(com.aerospace.procurement_service.event.MaterialShortageEvent event) {
        Supplier supplier = null;
        if (event.getPreferredSupplierId() != null) {
            try {
                Supplier preferred = supplierService.getSupplierById(event.getPreferredSupplierId());
                if ("CERTIFIED".equalsIgnoreCase(preferred.getCertificationStatus()) && preferred.isActive()) {
                    supplier = preferred;
                } else {
                    log.warn("Preferred supplier {} for part {} is not CERTIFIED/active — falling back",
                            event.getPreferredSupplierId(), event.getSkuCode());
                }
            } catch (Exception ex) {
                log.warn("Preferred supplier {} not found for part {} — falling back: {}",
                        event.getPreferredSupplierId(), event.getSkuCode(), ex.getMessage());
            }
        }
        if (supplier == null) {
            supplier = firstCertifiedSupplier();
        }
        if (supplier == null) {
            log.warn("Cannot auto-raise PO for WO {} shortage — no certified supplier available for part {}",
                    event.getWoNumber(), event.getSkuCode());
            return;
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setOrderDate(LocalDateTime.now());
        po.setDeliveryDate(LocalDateTime.now().plusDays(7));
        po.setStatus(POStatus.DRAFT);
        po.setPoNumber(documentSequenceService.generate("PO"));
        po.setCurrency("USD");
        po.setRemarks("AUTO-RAISED: shortage for WO " + event.getWoNumber()
                + " — verify price before submitting.");
        po.setSourceWorkOrderId(event.getWorkOrderId());
        po.setSourceWoNumber(event.getWoNumber());

        POLineItem item = new POLineItem();
        item.setPurchaseOrder(po);
        item.setSkuCode(event.getSkuCode());
        item.setQuantity(event.getShortfallQty());
        item.setUnitPrice(BigDecimal.ZERO);
        item.setTotalPrice(BigDecimal.ZERO);
        item.setUom("PCS");

        po.setItems(java.util.List.of(item));
        po.setTotalAmount(item.getTotalPrice());

        poRepository.save(po);
        log.info("📢 [PROCUREMENT AUTO] Created draft PO {} for WO {} shortage on part {} (qty {})",
                po.getPoNumber(), event.getWoNumber(), event.getSkuCode(), event.getShortfallQty());
    }

    // ── APPLY QA OUTCOME TO PO LINE ITEM ─────────────────────────────────────
    // Kafka-driven, like the two auto-raise handlers above — closes the loop so a
    // PO's line shows how much of what was received actually passed QA.
    @Transactional
    @Override
    public void applyQaOutcome(com.aerospace.procurement_service.event.QAStatusChangedEvent event) {
        if (event.getPoNumber() == null || event.getPoNumber().isBlank()) {
            return; // not traceable to a PO (e.g. manual stock add) — nothing to update
        }

        PurchaseOrder po = poRepository.findByPoNumber(event.getPoNumber()).orElse(null);
        if (po == null) {
            log.warn("QA outcome for PO {} — no matching purchase order found", event.getPoNumber());
            return;
        }

        POLineItem line = po.getItems().stream()
                .filter(i -> i.getSkuCode().equals(event.getSkuCode()))
                .findFirst()
                .orElse(null);
        if (line == null) {
            log.warn("QA outcome for PO {} — no line item for SKU {}", event.getPoNumber(), event.getSkuCode());
            return;
        }

        // Split quantities drive this directly rather than branching on newStatus — a
        // PARTIALLY_APPROVED decision moves both buckets in the same event.
        int approvedQty = event.getApprovedQty() != null ? event.getApprovedQty() : 0;
        int rejectedQty = event.getRejectedQty() != null ? event.getRejectedQty() : 0;
        if (approvedQty > 0) {
            line.setAcceptedQuantity((line.getAcceptedQuantity() != null ? line.getAcceptedQuantity() : 0) + approvedQty);
        }
        if (rejectedQty > 0) {
            line.setRejectedQuantity((line.getRejectedQuantity() != null ? line.getRejectedQuantity() : 0) + rejectedQty);
            line.setNcrNumber(event.getNcrNumber());
        }
        poRepository.save(po);
        log.info("📢 [PROCUREMENT] Applied QA outcome {} (approved {}, rejected {}) to PO {} line {}",
                event.getNewStatus(), approvedQty, rejectedQty, event.getPoNumber(), event.getSkuCode());
    }

    private Supplier firstCertifiedSupplier() {
        return supplierService.getAllSuppliers().stream()
                .filter(s -> "CERTIFIED".equalsIgnoreCase(s.getCertificationStatus()) && s.isActive())
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public void createFromAwardedQuotation(PurchaseRequisition pr, RequestForQuotation rfq, Quotation winner) {
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(winner.getSupplier());
        po.setOrderDate(LocalDateTime.now());
        po.setDeliveryDate(LocalDateTime.now().plusDays(winner.getLeadTimeDays()));
        po.setStatus(POStatus.DRAFT);
        po.setPoNumber(documentSequenceService.generate("PO"));
        po.setCurrency("USD");
        po.setRemarks("Raised from awarded RFQ " + rfq.getRfqNumber());
        po.setSourceWorkOrderId(pr.getSourceWorkOrderId());
        po.setSourceWoNumber(pr.getSourceWoNumber());
        po.setSourcePurchaseRequisitionId(pr.getId());
        po.setSourceRfqId(rfq.getId());
        po.setSourceQuotationId(winner.getId());

        POLineItem item = new POLineItem();
        item.setPurchaseOrder(po);
        item.setSkuCode(pr.getSkuCode());
        item.setQuantity(rfq.getQuantity());
        item.setUnitPrice(winner.getUnitPrice());
        item.setTotalPrice(winner.getTotalPrice());
        item.setUom("PCS");

        po.setItems(java.util.List.of(item));
        po.setTotalAmount(item.getTotalPrice());

        poRepository.save(po);
        log.info("📢 [PROCUREMENT] Created real PO {} from awarded RFQ {} — supplier {}, price {}",
                po.getPoNumber(), rfq.getRfqNumber(), winner.getSupplier().getName(), winner.getUnitPrice());
    }

    @Override
    public List<PurchaseOrderResponse> getForSupplier(Long supplierId) {
        return poRepository.findBySupplier_IdOrderByOrderDateDesc(supplierId).stream()
                .map(poMapper::toDTO)
                .collect(Collectors.toList());
    }
}
