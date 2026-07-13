package com.aerospace.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialReceiptResponse {
    private Long id;
    private String mrNumber;
    private String poNumber;
    private String supplierName;
    private String challanNumber;
    private String transportName;
    private String receivedBy;
    private LocalDateTime receivedAt;
    private String status;
    private List<MaterialReceiptItemResponse> items;
}
