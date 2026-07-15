package com.aerospace.procurement_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DebitNoteResponse {
    private Long id;
    private String debitNoteNumber;
    private Long purchaseOrderId;
    private String poNumber;
    private String supplierName;
    private String skuCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String ncrNumber;
    private String batchNumber;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
}