package com.aerospace.reporting_service.event;

import lombok.Data;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private LocalDateTime completedAt;
}