package com.aerospace.inventory_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class MaterialReceiptRequest {
    private String poNumber;
    private String supplierName;
    private String challanNumber;
    private String transportName;
    private String receivedBy;
    private List<MaterialReceiptItemRequest> items;
}
