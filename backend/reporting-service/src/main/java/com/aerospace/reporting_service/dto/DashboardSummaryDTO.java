package com.aerospace.reporting_service.dto;
import lombok.Data;

@Data
public class DashboardSummaryDTO {
    private double inventoryValue;
    private long totalWorkOrders;
    private double completionRate;
    private long openProductionOrders;
    private long pendingQAItems;
    private long lowStockCount;
}