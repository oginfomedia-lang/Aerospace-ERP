package com.aerospace.production_service.dto;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {

    private long totalOrders;
    private long plannedOrders;
    private long inProductionOrders;
    private long completedOrders;
    private long onHoldOrders;
    private long cancelledOrders;
    private long qualityCheckOrders;

    // Quantity metrics
    private long totalPlannedQty;
    private long totalProducedQty;
    private long totalRejectedQty;

    // Efficiency
    private double overallCompletionRate;   // (produced / planned) * 100
    private double overallRejectionRate;    // (rejected / produced) * 100

    // Breakdown by priority
    private Map<String, Long> ordersByPriority;

    // Breakdown by status
    private Map<String, Long> ordersByStatus;

    // Breakdown by product
    private Map<String, Long> ordersByProduct;
}
