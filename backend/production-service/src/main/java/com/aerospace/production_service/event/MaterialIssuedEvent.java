package com.aerospace.production_service.event;

import lombok.*;

// Consumed from inventory-service topic: material-issued-events
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialIssuedEvent {
    // Set only when this issue was made against a specific Work Order line —
    // lets us match it to an open ProductionOrder for that same WO.
    private Long workOrderId;
    private String woNumber;
    private Long lineItemId;

    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer quantityIssued;
    private Integer remainingQuantity;
    private String issuedTo;
    private String issuedBy;
    private String location;
}
