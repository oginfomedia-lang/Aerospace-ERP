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

    // Set only when this PO was auto-raised for a Work Order material shortage
    private Long sourceWorkOrderId;
    private String sourceWoNumber;

    // Optional per-PO overrides for the printed document — null falls back to the
    // Organization Profile default at render time.
    private String shipToName;
    private String shipToAddress;
    private String shipToState;
    private String shipToStateCode;
    private String paymentTerms;
    private String payment;
    private String deliveryTerms;
    private String exciseDuty;
    private String termsAndConditions;

    // Approval chain (PUR-BR-001)
    private Integer currentApprovalLevel;
    private int documentVersion;
    private String pendingApproverRole; // role required to act on the current level, if any

    // PUR-BR-005: non-blocking warning surfaced on create when a likely-duplicate open PO exists
    private String duplicateWarning;
}
