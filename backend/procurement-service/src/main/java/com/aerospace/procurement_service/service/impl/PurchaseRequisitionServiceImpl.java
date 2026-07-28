package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.RequisitionDTOs.CreatePurchaseRequisitionRequest;
import com.aerospace.procurement_service.dto.RequisitionDTOs.PurchaseRequisitionResponse;
import com.aerospace.procurement_service.entity.PRSourceType;
import com.aerospace.procurement_service.entity.PRStatus;
import com.aerospace.procurement_service.entity.PurchaseRequisition;
import com.aerospace.procurement_service.entity.RequestForQuotation;
import com.aerospace.procurement_service.event.MaterialShortageEvent;
import com.aerospace.procurement_service.event.PrStatusEvent;
import com.aerospace.procurement_service.event.StockLowEvent;
import com.aerospace.procurement_service.event.producer.ProcurementEventPublisher;
import com.aerospace.procurement_service.exception.ResourceNotFoundException;
import com.aerospace.procurement_service.repository.PurchaseRequisitionRepository;
import com.aerospace.procurement_service.repository.RequestForQuotationRepository;
import com.aerospace.procurement_service.service.DocumentSequenceService;
import com.aerospace.procurement_service.service.PurchaseRequisitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequisitionServiceImpl implements PurchaseRequisitionService {

    private final PurchaseRequisitionRepository prRepository;
    private final RequestForQuotationRepository rfqRepository;
    private final DocumentSequenceService documentSequenceService;
    private final ProcurementEventPublisher eventPublisher;

    private static final List<PRStatus> TERMINAL = List.of(PRStatus.PO_RAISED, PRStatus.CANCELLED);

    @Override
    @Transactional
    public void createFromShortage(MaterialShortageEvent event) {
        if (!prRepository.findByPartIdAndStatusNotIn(event.getPartId(), TERMINAL).isEmpty()) {
            log.info("[PR] Open PR already exists for partId={} — skipping duplicate (WO {})",
                    event.getPartId(), event.getWoNumber());
            return;
        }
        PurchaseRequisition pr = PurchaseRequisition.builder()
                .prNumber(documentSequenceService.generate("PR"))
                .partId(event.getPartId())
                .partName(event.getPartName())
                .skuCode(event.getSkuCode())
                .quantityNeeded(event.getShortfallQty())
                .sourceType(PRSourceType.WORK_ORDER_SHORTAGE)
                .sourceWorkOrderId(event.getWorkOrderId())
                .sourceWoNumber(event.getWoNumber())
                .status(PRStatus.PENDING_APPROVAL)
                .remarks("Auto-raised: shortage on Work Order " + event.getWoNumber())
                .createdBy("SYSTEM")
                .build();
        PurchaseRequisition saved = prRepository.save(pr);
        publishSubmitted(saved);
        log.info("[PR] Created {} from WO {} shortage (part {}, qty {})",
                saved.getPrNumber(), event.getWoNumber(), event.getSkuCode(), event.getShortfallQty());
    }

    @Override
    @Transactional
    public void createFromStockLow(StockLowEvent event) {
        if (!prRepository.findByPartIdAndStatusNotIn(event.getPartId(), TERMINAL).isEmpty()) {
            log.info("[PR] Open PR already exists for partId={} — skipping duplicate (low stock)",
                    event.getPartId());
            return;
        }
        PurchaseRequisition pr = PurchaseRequisition.builder()
                .prNumber(documentSequenceService.generate("PR"))
                .partId(event.getPartId())
                .partName(event.getPartName())
                .skuCode(event.getSkuCode())
                .quantityNeeded(event.getReorderLevel() * 2)
                .sourceType(PRSourceType.STOCK_LOW)
                .status(PRStatus.PENDING_APPROVAL)
                .remarks("Auto-raised: stock below reorder level (" + event.getCurrentQuantity()
                        + " < " + event.getReorderLevel() + ")")
                .createdBy("SYSTEM")
                .build();
        PurchaseRequisition saved = prRepository.save(pr);
        publishSubmitted(saved);
        log.info("[PR] Created {} from low stock (part {}, qty {})",
                saved.getPrNumber(), event.getSkuCode(), saved.getQuantityNeeded());
    }

    @Override
    @Transactional
    public PurchaseRequisitionResponse createManual(CreatePurchaseRequisitionRequest request) {
        PurchaseRequisition pr = PurchaseRequisition.builder()
                .prNumber(documentSequenceService.generate("PR"))
                .partId(request.getPartId())
                .partName(request.getPartName())
                .skuCode(request.getSkuCode())
                .quantityNeeded(request.getQuantityNeeded())
                .requiredByDate(request.getRequiredByDate())
                .sourceType(PRSourceType.MANUAL)
                .status(PRStatus.PENDING_APPROVAL)
                .remarks(request.getRemarks())
                .createdBy("PROCUREMENT_OFFICER") // swap for the real authenticated username if available
                .build();
        PurchaseRequisition saved = prRepository.save(pr);
        publishSubmitted(saved);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public PurchaseRequisitionResponse approve(Long id, String approverUsername) {
        PurchaseRequisition pr = findOrThrow(id);
        if (pr.getStatus() != PRStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "PR " + pr.getPrNumber() + " is not awaiting approval (current: " + pr.getStatus() + ").");
        }
        pr.setStatus(PRStatus.DRAFT);
        pr.setApprovedBy(approverUsername);
        pr.setApprovedAt(LocalDateTime.now());
        PurchaseRequisition saved = prRepository.save(pr);

        eventPublisher.publishPrStatusChanged(PrStatusEvent.builder()
                .prNumber(saved.getPrNumber())
                .partName(saved.getPartName())
                .quantityNeeded(saved.getQuantityNeeded())
                .status("APPROVED")
                .actor(approverUsername)
                .build());

        log.info("[PR] {} approved by {}", saved.getPrNumber(), approverUsername);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public PurchaseRequisitionResponse reject(Long id, String approverUsername, String remarks) {
        PurchaseRequisition pr = findOrThrow(id);
        if (pr.getStatus() != PRStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "PR " + pr.getPrNumber() + " is not awaiting approval (current: " + pr.getStatus() + ").");
        }
        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException("Rejection remarks are mandatory.");
        }
        pr.setStatus(PRStatus.CANCELLED);
        pr.setApprovedBy(approverUsername);
        pr.setApprovedAt(LocalDateTime.now());
        pr.setRejectionRemarks(remarks);
        PurchaseRequisition saved = prRepository.save(pr);

        eventPublisher.publishPrStatusChanged(PrStatusEvent.builder()
                .prNumber(saved.getPrNumber())
                .partName(saved.getPartName())
                .quantityNeeded(saved.getQuantityNeeded())
                .status("REJECTED")
                .actor(approverUsername)
                .remarks(remarks)
                .build());

        log.info("[PR] {} rejected by {}: {}", saved.getPrNumber(), approverUsername, remarks);
        return toDTO(saved);
    }

    @Override
    public List<PurchaseRequisitionResponse> getAll() {
        return prRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDTO).toList();
    }

    @Override
    public PurchaseRequisitionResponse getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    private PurchaseRequisition findOrThrow(Long id) {
        return prRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Requisition not found: " + id));
    }

    private void publishSubmitted(PurchaseRequisition pr) {
        eventPublisher.publishPrStatusChanged(PrStatusEvent.builder()
                .prNumber(pr.getPrNumber())
                .partName(pr.getPartName())
                .quantityNeeded(pr.getQuantityNeeded())
                .status("PENDING_APPROVAL")
                .actor(pr.getCreatedBy())
                .build());
    }

    private PurchaseRequisitionResponse toDTO(PurchaseRequisition pr) {
        List<RequestForQuotation> rfqs = rfqRepository.findByPurchaseRequisition_IdOrderByCreatedAtDesc(pr.getId());
        RequestForQuotation latestRfq = rfqs.isEmpty() ? null : rfqs.get(0);

        return PurchaseRequisitionResponse.builder()
                .id(pr.getId())
                .prNumber(pr.getPrNumber())
                .partId(pr.getPartId())
                .partName(pr.getPartName())
                .skuCode(pr.getSkuCode())
                .quantityNeeded(pr.getQuantityNeeded())
                .requiredByDate(pr.getRequiredByDate())
                .sourceType(pr.getSourceType())
                .sourceWorkOrderId(pr.getSourceWorkOrderId())
                .sourceWoNumber(pr.getSourceWoNumber())
                .status(pr.getStatus())
                .remarks(pr.getRemarks())
                .createdAt(pr.getCreatedAt())
                .latestRfqId(latestRfq == null ? null : latestRfq.getId())
                .latestRfqNumber(latestRfq == null ? null : latestRfq.getRfqNumber())
                .approvedBy(pr.getApprovedBy())
                .approvedAt(pr.getApprovedAt())
                .rejectionRemarks(pr.getRejectionRemarks())
                .build();
    }
}
