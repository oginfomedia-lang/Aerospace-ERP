package com.aerospace.production_service.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderCompletedEvent {
    private Long productionOrderId;
    private String poNumber;
    private String productName;
    private String productCode;
    private String aircraftType;
    private Integer plannedQuantity;
    private Integer producedQuantity;
    private Integer rejectedQuantity;
    private Boolean qualityPassed;
    private String qualityNotes;
    private String supervisorName;
    private String closureNotes;
    private String batchNumber;
    private Long outputPartId;        // FK into inventory-service's Part — required before completion
    private String outputPartNumber;  // denormalized Part.skuCode, for display
}
