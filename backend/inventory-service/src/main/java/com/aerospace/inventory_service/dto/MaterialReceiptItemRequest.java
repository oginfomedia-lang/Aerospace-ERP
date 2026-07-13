package com.aerospace.inventory_service.dto;

import lombok.Data;

@Data
public class MaterialReceiptItemRequest {
    private String skuCode;
    private Integer quantity;
    private String uom;
}
