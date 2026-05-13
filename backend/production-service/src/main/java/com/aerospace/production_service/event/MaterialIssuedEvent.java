package com.aerospace.production_service.event;

import lombok.*;

// Consumed from inventory-service topic: material-issued-events
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialIssuedEvent {
    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer quantityIssued;
    private Integer remainingQuantity;
    private String issuedTo;
    private String issuedBy;
    private String location;
}
