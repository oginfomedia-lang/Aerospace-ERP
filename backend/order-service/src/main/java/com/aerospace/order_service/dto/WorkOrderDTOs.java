package com.aerospace.order_service.dto;

import com.aerospace.order_service.entity.MaterialLineItem;
import com.aerospace.order_service.entity.Priority;
import com.aerospace.order_service.entity.WorkOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ── Request DTOs ─────────────────────────────────────────────────────────────

public class WorkOrderDTOs {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateWorkOrderRequest {
        @NotBlank(message = "Aircraft ID is required")
        private String aircraftId;

        private String aircraftType;

        @NotBlank(message = "Task description is required")
        private String taskDescription;

        private String taskCode;

        @NotNull(message = "Priority is required")
        private Priority priority;

        private String assignedTo;
        private String createdBy;
        private String location;
        private LocalDateTime dueDate;
        private String remarks;

        // Material line items to add at creation time
        private List<MaterialLineItemRequest> materialItems;

        // What this task/job is expected to produce, if anything (optional).
        private Long outputPartId;
        private String outputPartNumber;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialLineItemRequest {
        @NotNull(message = "Part ID is required")
        private Long partId;

        private String partName;
        private String partNumber;

        @NotNull(message = "Quantity required is mandatory")
        private Integer quantityRequired;

        private String remarks;

        // Snapshotted from the Part item master at the time this line was added
        // (same rationale as POLineItem.unitPrice — a point-in-time snapshot,
        // not a live join back to inventory-service).
        private BigDecimal unitPrice;
        private BigDecimal taxRatePercent;
        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateWorkOrderRequest {
        private String taskDescription;
        private String taskCode;
        private Priority priority;
        private String assignedTo;
        private String location;
        private LocalDateTime dueDate;
        private String remarks;
        private Long outputPartId;
        private String outputPartNumber;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StatusTransitionRequest {
        @NotNull(message = "New status is required")
        private WorkOrderStatus newStatus;

        private String changedBy;
        private String remarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IssueMaterialRequest {
        @NotNull(message = "Line item ID is required")
        private Long lineItemId;

        @NotNull(message = "Quantity issued is required")
        private Integer quantityIssued;

        private String batchNumber;
        private String issuedBy;
    }

    // ── Response DTOs ─────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WorkOrderResponse {
        private Long id;
        private String woNumber;
        private String aircraftId;
        private String aircraftType;
        private String taskDescription;
        private String taskCode;
        private WorkOrderStatus status;
        private Priority priority;
        private String assignedTo;
        private String createdBy;
        private String location;
        private LocalDateTime dueDate;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String remarks;
        private String closureNotes;
        private Long outputPartId;
        private String outputPartNumber;
        private List<MaterialLineItemResponse> materialLineItems;
        private int totalPartsRequired;
        private int totalPartsIssued;
        private boolean allMaterialReady;
        // Sum of quantityRequired * unitPrice * (1 + taxRatePercent/100) across all
        // lines that have a snapshotted rate — an estimate, not an invoiced total.
        private BigDecimal totalMaterialCost;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialLineItemResponse {
        private Long id;
        private Long partId;
        private String partName;
        private String partNumber;
        private String batchNumber;
        private Integer quantityRequired;
        private Integer quantityIssued;
        private MaterialLineItem.LineItemStatus status;
        private String remarks;
        private BigDecimal unitPrice;
        private BigDecimal taxRatePercent;
        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StatusHistoryResponse {
        private Long id;
        private String woNumber;
        private WorkOrderStatus fromStatus;
        private WorkOrderStatus toStatus;
        private String changedBy;
        private String remarks;
        private LocalDateTime changedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WorkOrderNeedingPartResponse {
        private Long workOrderId;
        private String woNumber;
        private Long lineItemId;
        private String taskDescription;
        private Integer quantityRequired;
        private Integer quantityIssued;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardStatsResponse {
        private long totalWorkOrders;
        private long draft;
        private long open;
        private long inProgress;
        private long onHold;
        private long completed;
        private long cancelled;
        private long criticalPriority;   // AOG count
        private long overdueCount;       // past dueDate and not completed
    }
}