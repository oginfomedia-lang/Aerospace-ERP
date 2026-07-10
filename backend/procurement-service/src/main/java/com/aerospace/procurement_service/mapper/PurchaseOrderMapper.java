package com.aerospace.procurement_service.mapper;

import com.aerospace.procurement_service.dto.POApprovalHistoryDTO;
import com.aerospace.procurement_service.dto.POLineItemDTO;
import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;
import com.aerospace.procurement_service.entity.POApprovalHistory;
import com.aerospace.procurement_service.entity.POLineItem;
import com.aerospace.procurement_service.entity.PurchaseOrder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PurchaseOrderMapper {

    public PurchaseOrder toEntity(PurchaseOrderRequest dto) {
        if (dto == null) return null;

        PurchaseOrder po = PurchaseOrder.builder()
                .deliveryDate(dto.getDeliveryDate())
                .currency(dto.getCurrency())
                .remarks(dto.getRemarks())
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
                .build();
    }

    public PurchaseOrderResponse toDTO(PurchaseOrder po) {
        if (po == null) return null;

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
                .currentApprovalLevel(po.getCurrentApprovalLevel())
                .documentVersion(po.getDocumentVersion())
                .build();
    }

    public POApprovalHistoryDTO toApprovalHistoryDTO(POApprovalHistory h) {
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
                .build();
    }
}
