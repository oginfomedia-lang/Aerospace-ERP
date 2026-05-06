package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialIssuedEvent {
    // Legacy fields — kept so existing code still compiles
    private Long materialId;
    private String workOrderId;

    // ── New fields required by notification-service ──────────────────────────
    private Long partId;
    private String partName;
    private String batchNumber;
    private int quantityIssued;
    private int remainingQuantity;
    private String issuedTo;
    private String issuedBy;
    private String location;
}