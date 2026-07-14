package com.aerospace.inventory_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PartRequestDTO {
    private String name;
    private String serialNumber;
    private String batchNumber;
    private Integer quantity;
    private String location;
    private String partType;
    private String criticality;
    private Integer reorderLevel;
    private String status;    // Maps to qaStatus in Entity
    private String qaRemarks; // Added to capture remarks from React form

    // Item-master reference pricing — shown when adding this part to a Work
    // Order; distinct from a PO's own negotiated unitPrice.
    private BigDecimal unitPrice;
    private BigDecimal taxRatePercent;
    private String hsnCode;
    private String description;

    // Planning levels (ITM-BR-006)
    private Integer minStock;
    private Integer maxStock;
    private Integer safetyStock;
    private Integer leadTimeDays;

    // Item lifecycle status: ACTIVE, INACTIVE, DISCONTINUED
    private String itemStatus;

    // Tracking flags (ITM-BR-002, ITM-BR-003, GRN-FR-003)
    private Boolean batchRequired;
    private Boolean serialRequired;
    private Boolean expiryTracking;
    private Integer shelfLifeDays;
    private Boolean qcRequired;

    // Soft reference to procurement-service's Supplier — used for auto-raising a
    // shortage PO when a Work Order needs more than what's currently APPROVED.
    private Long preferredSupplierId;
}
