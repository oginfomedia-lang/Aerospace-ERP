package com.aerospace.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// ── Events PUBLISHED by work-order-service ───────────────────────────────────

public class WorkOrderEvents {

    /** One material line's demand — enough for a consumer to check availability elsewhere. */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialLineSummary {
        private Long partId;
        private Integer quantityRequired;
    }

    /**
     * Snapshot of one fully/partially-issued material line, carried ONLY on the
     * OPEN→IN_PROGRESS WorkOrderStatusChangedEvent — lets production-service auto-create
     * a ProductionOrder + its BOM (ProductionMaterial rows) without a synchronous
     * callback back into order-service (this repo is Kafka-only cross-service).
     */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialLineSnapshot {
        private Long partId;
        private String partName;
        private String partNumber;
        private String batchNumber;
        private Integer quantityRequired;
        private Integer quantityIssued;
    }

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
        // Lets consumers (e.g. inventory-service) check each line's stock availability
        // without a synchronous callback — this repo is Kafka-only, no cross-service REST.
        private List<MaterialLineSummary> materialLines;
    }

    /** Published on every status transition */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WorkOrderStatusChangedEvent {
        private Long workOrderId;
        private String woNumber;
        private String aircraftId;
        private String aircraftType;
        private String taskDescription;
        private String taskCode;
        private String priority;
        private String fromStatus;
        private String toStatus;
        private String changedBy;
        private String remarks;
        private LocalDateTime changedAt;

        // What this WO is expected to produce, if anything — carried on every transition
        // so production-service can set it on the auto-created ProductionOrder immediately.
        private Long outputPartId;
        private String outputPartNumber;

        // Populated ONLY when toStatus == IN_PROGRESS (all material lines issued) —
        // null/empty on every other transition. See MaterialLineSnapshot.
        private List<MaterialLineSnapshot> materialLineItems;
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