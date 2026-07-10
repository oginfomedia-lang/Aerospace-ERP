package com.aerospace.inventory_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartResponseDTO {
    private Long id;
    private String name;
    private String serialNumber;
    private String batchNumber;   // Added — APPROVED-batch summary, see `balances` for full per-batch state
    private Integer quantity;     // Added — APPROVED-only quantity (usable/available stock)
    private String location;
    private String partType;
    private String criticality;   // Added
    private Integer reorderLevel; // Added
    private String status;        // Maps to qaStatus — APPROVED-first summary, see `balances` for full state
    private String qaRemarks;     // Added
    private LocalDateTime expiryDate;

    // Authoritative per-batch/status breakdown (a part can have APPROVED, PENDING_QA,
    // RESERVED and REJECTED stock simultaneously, across different batches). Anything
    // that needs to know about non-APPROVED stock (e.g. the QA queue) must read this
    // list rather than the collapsed top-level fields above.
    private List<StockBalanceDTO> balances;

    // Planning levels (ITM-BR-006)
    private Integer minStock;
    private Integer maxStock;
    private Integer safetyStock;
    private Integer leadTimeDays;

    // Item lifecycle status: ACTIVE, INACTIVE, DISCONTINUED
    private String itemStatus;

    // Tracking flags (ITM-BR-002, ITM-BR-003, GRN-FR-003)
    private boolean batchRequired;
    private boolean serialRequired;
    private boolean expiryTracking;
    private Integer shelfLifeDays;
    private boolean qcRequired;

    // Set only on GRN receipt responses (GRN-FR-001: system-generated, never user-typed)
    private String grnNumber;
}
