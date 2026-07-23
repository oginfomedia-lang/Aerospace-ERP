package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.RequisitionDTOs.CreatePurchaseRequisitionRequest;
import com.aerospace.procurement_service.dto.RequisitionDTOs.PurchaseRequisitionResponse;
import com.aerospace.procurement_service.entity.PRSourceType;
import com.aerospace.procurement_service.entity.PRStatus;
import com.aerospace.procurement_service.entity.PurchaseRequisition;
import com.aerospace.procurement_service.event.MaterialShortageEvent;
import com.aerospace.procurement_service.event.StockLowEvent;
import com.aerospace.procurement_service.exception.ResourceNotFoundException;
import com.aerospace.procurement_service.repository.PurchaseRequisitionRepository;
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
    private final DocumentSequenceService documentSequenceService;

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
                .status(PRStatus.DRAFT)
                .remarks("Auto-raised: shortage on Work Order " + event.getWoNumber())
                .createdBy("SYSTEM")
                .build();
        prRepository.save(pr);
        log.info("[PR] Created {} from WO {} shortage (part {}, qty {})",
                pr.getPrNumber(), event.getWoNumber(), event.getSkuCode(), event.getShortfallQty());
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
                .status(PRStatus.DRAFT)
                .remarks("Auto-raised: stock below reorder level (" + event.getCurrentQuantity()
                        + " < " + event.getReorderLevel() + ")")
                .createdBy("SYSTEM")
                .build();
        prRepository.save(pr);
        log.info("[PR] Created {} from low stock (part {}, qty {})",
                pr.getPrNumber(), event.getSkuCode(), pr.getQuantityNeeded());
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
                .status(PRStatus.DRAFT)
                .remarks(request.getRemarks())
                .createdBy("PROCUREMENT_OFFICER") // swap for the real authenticated username if available
                .build();
        return toDTO(prRepository.save(pr));
    }

    @Override
    public List<PurchaseRequisitionResponse> getAll() {
        return prRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDTO).toList();
    }

    @Override
    public PurchaseRequisitionResponse getById(Long id) {
        return toDTO(prRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Requisition not found: " + id)));
    }

    private PurchaseRequisitionResponse toDTO(PurchaseRequisition pr) {
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
                .build();
    }
}