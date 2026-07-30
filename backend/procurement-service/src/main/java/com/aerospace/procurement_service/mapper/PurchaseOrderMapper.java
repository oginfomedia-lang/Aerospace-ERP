package com.aerospace.procurement_service.mapper;

import com.aerospace.procurement_service.dto.POApprovalHistoryDTO;
import com.aerospace.procurement_service.dto.POLineItemDTO;
import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;
import com.aerospace.procurement_service.entity.POLineItem;
import com.aerospace.procurement_service.entity.PurchaseOrder;
import com.aerospace.common.approval.ApprovalHistory;
import com.aerospace.common.approval.ApprovalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PurchaseOrderMapper {

    private static final String MODULE = "PURCHASE_ORDER";

    private final ApprovalHistoryRepository approvalHistoryRepository;

    public PurchaseOrder toEntity(PurchaseOrderRequest dto) {
        if (dto == null) return null;

        PurchaseOrder po = PurchaseOrder.builder()
                .deliveryDate(dto.getDeliveryDate())
                .currency(dto.getCurrency())
                .remarks(dto.getRemarks())
                .shipToName(dto.getShipToName())
                .shipToAddress(dto.getShipToAddress())
                .shipToState(dto.getShipToState())
                .shipToStateCode(dto.getShipToStateCode())
                .paymentTerms(dto.getPaymentTerms())
                .payment(dto.getPayment())
                .deliveryTerms(dto.getDeliveryTerms())
                .exciseDuty(dto.getExciseDuty())
                .termsAndConditions(dto.getTermsAndConditions())
                .build();

        if (dto.getItems() != null) {
            List<POLineItem> items = dto.getItems().stream()
                    .map(itemDto -> toLineItemEntity(itemDto, po))
                    .collect(Collectors.toList());
            po.setItems(items);
        }
        return po;
    }

    public POLineItem toLineItemEntity(POLineItemDTO dto, PurchaseOrder po) {
        if (dto == null) return null;
        
        return POLineItem.builder()
                .purchaseOrder(po)
                .skuCode(dto.getSkuCode())
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .uom(dto.getUom())
                .hsnCode(dto.getHsnCode())
                .taxRatePercent(dto.getTaxRatePercent())
                .description(dto.getDescription())
                .replacementForNcrNumber(dto.getReplacementForNcrNumber())
                .build();
    }

    public PurchaseOrderResponse toDTO(PurchaseOrder po) {
        if (po == null) return null;

        // Role required to act on the PO's current pending level, if it's awaiting one —
        // looked up here (rather than passed in by each caller) so every read path (list,
        // submit, approve) reports it consistently instead of only the action that set it.
        String pendingApproverRole = po.getCurrentApprovalLevel() != null
                ? approvalHistoryRepository
                        .findByModuleCodeAndDocumentIdOrderByApprovalLevelAsc(MODULE, po.getId())
                        .stream()
                        .filter(h -> h.getApprovalLevel() == po.getCurrentApprovalLevel()
                                && h.getDocumentVersion() == po.getDocumentVersion())
                        .findFirst()
                        .map(ApprovalHistory::getApproverRole)
                        .orElse(null)
                : null;

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : null)
                .status(po.getStatus() != null ? po.getStatus().name() : null)
                .orderDate(po.getOrderDate())
                .deliveryDate(po.getDeliveryDate())
                .totalAmount(po.getTotalAmount())
                .currency(po.getCurrency())
                .items(po.getItems() != null ? po.getItems().stream().map(this::toLineItemDTO).collect(Collectors.toList()) : null)
                .remarks(po.getRemarks())
                .sourceWorkOrderId(po.getSourceWorkOrderId())
                .sourceWoNumber(po.getSourceWoNumber())
                .shipToName(po.getShipToName())
                .shipToAddress(po.getShipToAddress())
                .shipToState(po.getShipToState())
                .shipToStateCode(po.getShipToStateCode())
                .paymentTerms(po.getPaymentTerms())
                .payment(po.getPayment())
                .deliveryTerms(po.getDeliveryTerms())
                .exciseDuty(po.getExciseDuty())
                .termsAndConditions(po.getTermsAndConditions())
                .currentApprovalLevel(po.getCurrentApprovalLevel())
                .documentVersion(po.getDocumentVersion())
                .pendingApproverRole(pendingApproverRole)
                .build();
    }

    public POApprovalHistoryDTO toApprovalHistoryDTO(ApprovalHistory h) {
        if (h == null) return null;

        return POApprovalHistoryDTO.builder()
                .approvalLevel(h.getApprovalLevel())
                .approverRole(h.getApproverRole())
                .status(h.getStatus() != null ? h.getStatus().name() : null)
                .approverUsername(h.getApproverUsername())
                .approvedAt(h.getApprovedAt())
                .remarks(h.getRemarks())
                .digitalSignatureHash(h.getDigitalSignatureHash())
                .documentVersion(h.getDocumentVersion())
                .escalated(h.isEscalated())
                .build();
    }

    public POLineItemDTO toLineItemDTO(POLineItem item) {
        if (item == null) return null;

        return POLineItemDTO.builder()
                .skuCode(item.getSkuCode())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .uom(item.getUom())
                .hsnCode(item.getHsnCode())
                .taxRatePercent(item.getTaxRatePercent())
                .description(item.getDescription())
                .acceptedQuantity(item.getAcceptedQuantity())
                .rejectedQuantity(item.getRejectedQuantity())
                .receivedQuantity(item.getReceivedQuantity())
                .ncrNumber(item.getNcrNumber())
                .replacementForNcrNumber(item.getReplacementForNcrNumber())
                .build();
    }
}
