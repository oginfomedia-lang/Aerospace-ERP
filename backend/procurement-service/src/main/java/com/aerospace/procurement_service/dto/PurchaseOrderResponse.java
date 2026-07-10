package com.aerospace.procurement_service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PurchaseOrderResponse {
    private Long id;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private String status;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private BigDecimal totalAmount;
    private String currency;
    private List<POLineItemDTO> items;
    private String remarks;

    // Approval chain (PUR-BR-001)
    private Integer currentApprovalLevel;
    private int documentVersion;
    private String pendingApproverRole; // role required to act on the current level, if any

    // PUR-BR-005: non-blocking warning surfaced on create when a likely-duplicate open PO exists
    private String duplicateWarning;
}
