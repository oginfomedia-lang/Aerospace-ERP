package com.aerospace.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ── Events PUBLISHED by work-order-service ───────────────────────────────────

public class WorkOrderEvents {

    /** Published when a new work order is created */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WorkOrderCreatedEvent {
        private Long workOrderId;
        private String woNumber;
        private String aircraftId;
        private String taskDescription;
        private String priority;
        private String status;
        private String createdBy;
        private String location;
        private LocalDateTime createdAt;
    }

    /** Published on every status transition */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WorkOrderStatusChangedEvent {
        private Long workOrderId;
        private String woNumber;
        private String aircraftId;
        private String taskDescription;
        private String priority;
        private String fromStatus;
        private String toStatus;
        private String changedBy;
        private String remarks;
        private LocalDateTime changedAt;
    }

    /** Published when WO is completed */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WorkOrderCompletedEvent {
        private Long workOrderId;
        private String woNumber;
        private String aircraftId;
        private String taskDescription;
        private String completedBy;
        private String closureNotes;
        private LocalDateTime completedAt;
    }

    /** Published when material is requested from inventory */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialRequestEvent {
        private Long workOrderId;
        private String woNumber;
        private Long partId;
        private String partName;
        private String batchNumber;
        private Integer quantityRequested;
        private String requestedBy;
        private String location;
        private LocalDateTime requestedAt;
    }
}

// ── Event CONSUMED by work-order-service (from inventory-service) ─────────────

class QAStatusChangedEvent {
    // consumed to auto-update WO status if parts were rejected
    private Long partId;
    private String batchNumber;
    private String newStatus;
    private String partName;
}