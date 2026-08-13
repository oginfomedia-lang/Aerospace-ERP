package com.aerospace.sales_service.dto;

import com.aerospace.sales_service.entity.SalesOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

// §18.12: one Sales Order should answer "which batch, which shipment" — built entirely
// from data sales-service already owns (no cross-service calls needed for this core view).
@Data
@Builder
public class OrderTrackingDTO {
    private Long salesOrderId;
    private String orderNumber;
    private SalesOrderStatus status;
    private List<DispatchSummary> dispatches;
    private List<AllocationSummary> allocations;

    @Data
    @Builder
    public static class DispatchSummary {
        private String dispatchNumber;
        private String status;
        private String carrierName;
        private String trackingNumber;
        private java.time.LocalDateTime dispatchedAt;
    }

    @Data
    @Builder
    public static class AllocationSummary {
        private String partNumber;
        private String batchNumber;
        private Integer quantity;
        private String status;
    }
}
