package com.aerospace.reporting_service.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductionOrderCreatedEvent {
    private Long productionOrderId;
    private String poNumber;
    private String productName;
    private String productCode;
    private String aircraftType;
    private Integer plannedQuantity;
    private String priority;
    private String productionLine;
    private String assignedTeam;
    private String createdBy;
    private String batchNumber;
    private LocalDateTime createdAt;
}