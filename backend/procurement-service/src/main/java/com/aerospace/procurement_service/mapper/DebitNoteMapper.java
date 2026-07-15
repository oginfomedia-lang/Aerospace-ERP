package com.aerospace.procurement_service.mapper;

import com.aerospace.procurement_service.dto.DebitNoteResponse;
import com.aerospace.procurement_service.entity.DebitNote;
import org.springframework.stereotype.Component;

@Component
public class DebitNoteMapper {
    
    public DebitNoteResponse toDTO(DebitNote note){
        if (note == null) return null;

        return DebitNoteResponse.builder()
                .id(note.getId())
                .debitNoteNumber(note.getDebitNoteNumber())
                .purchaseOrderId(note.getPurchaseOrder() != null ? note.getPurchaseOrder().getId() : null)
                .poNumber(note.getPurchaseOrder() != null ? note.getPurchaseOrder().getPoNumber() : null)
                .supplierName(note.getSupplierName())
                .skuCode(note.getSkuCode())
                .quantity(note.getQuantity())
                .unitPrice(note.getUnitPrice())
                .amount(note.getAmount())
                .ncrNumber(note.getNcrNumber())
                .batchNumber(note.getBatchNumber())
                .status(note.getStatus() != null ? note.getStatus().name() : null)
                .remarks(note.getRemarks())
                .createdAt(note.getCreatedAt())
                .settledAt(note.getSettledAt())
                .build();
    }
}
