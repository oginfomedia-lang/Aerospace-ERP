package com.aerospace.inventory_service.event;
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
    private Long outputPartId;
    private String outputPartNumber;
}   
