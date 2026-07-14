package com.aerospace.inventory_service.mapper;

import com.aerospace.inventory_service.dto.MaterialReceiptItemResponse;
import com.aerospace.inventory_service.dto.MaterialReceiptResponse;
import com.aerospace.inventory_service.entity.MaterialReceipt;
import com.aerospace.inventory_service.entity.MaterialReceiptItem;
import org.springframework.stereotype.Component;

@Component
public class MaterialReceiptMapper {

    public MaterialReceiptResponse toDTO(MaterialReceipt receipt) {
        if (receipt == null) return null;

        return MaterialReceiptResponse.builder()
                .id(receipt.getId())
                .mrNumber(receipt.getMrNumber())
                .poNumber(receipt.getPoNumber())
                .supplierName(receipt.getSupplierName())
                .challanNumber(receipt.getChallanNumber())
                .transportName(receipt.getTransportName())
                .receivedBy(receipt.getReceivedBy())
                .receivedAt(receipt.getReceivedAt())
                .grnFormedAt(receipt.getGrnFormedAt())
                .status(receipt.getStatus().name())
                .items(receipt.getItems().stream().map(this::toItemDTO).toList())
                .build();
    }

    public MaterialReceiptItemResponse toItemDTO(MaterialReceiptItem item) {
        return MaterialReceiptItemResponse.builder()
                .id(item.getId())
                .skuCode(item.getSkuCode())
                .quantity(item.getQuantity())
                .uom(item.getUom())
                .grnNumber(item.getGrnNumber())
                .batchNumber(item.getBatchNumber())
                .location(item.getLocation())
                .expiryDate(item.getExpiryDate())
                .inspectionQty(item.getInspectionQty())
                .build();
    }
}
