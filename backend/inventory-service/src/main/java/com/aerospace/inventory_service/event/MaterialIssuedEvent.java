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
    // Set only when this issue was made against a specific Work Order line
    // (WorkOrderDetailModal's "Issue Material" flow) — null for ad-hoc issues
    // from the plain Inventory screen. order-service and production-service
    // use these to update their own records for the same issuance.
    private Long workOrderId;
    private String woNumber;
    private Long lineItemId;

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