package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// Local mirror of order-service's WorkOrderEvents.WorkOrderCreatedEvent — cross-service
// Kafka payloads in this repo are matched by JSON field name (see QAStatusUpdatedEvent),
// not a shared class, since each service deserializes with useTypeHeaders(false).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCreatedEvent {
    private Long workOrderId;
    private String woNumber;
    private String aircraftId;
    private String taskDescription;
    private String priority;
    private String status;
    private String createdBy;
    private String location;
    private LocalDateTime createdAt;
    private List<MaterialLineSummary> materialLines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialLineSummary {
        private Long partId;
        private Integer quantityRequired;
    }
}
