package com.aerospace.inventory_service.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PartRequestDTO {
    private String name;
    private String serialNumber;
    private String batchNumber;
    @PositiveOrZero
    private Integer quantity;
    private String location;
    private String partType;
    private String criticality;
    @PositiveOrZero
    private Integer reorderLevel;
    private String status;    // Maps to qaStatus in Entity
    @Size(max = 2000)
    private String qaRemarks; // Added to capture remarks from React form

    // Item-master reference pricing — shown when adding this part to a Work
    // Order; distinct from a PO's own negotiated unitPrice.
    @PositiveOrZero
    private BigDecimal unitPrice;
    @PositiveOrZero
    private BigDecimal taxRatePercent;
    private String hsnCode;
    @Size(max = 2000)
    private String description;

    // Planning levels (ITM-BR-006)
    @PositiveOrZero
    private Integer minStock;
    @PositiveOrZero
    private Integer maxStock;
    @PositiveOrZero
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
