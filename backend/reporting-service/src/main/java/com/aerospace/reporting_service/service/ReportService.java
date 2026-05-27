package com.aerospace.reporting_service.service;

import com.aerospace.reporting_service.dto.*;

import java.util.List;

public interface ReportService {
    DashboardSummaryDTO getDashboardSummary();
    List<InventoryValueDTO> getInventoryValueByPart();
    List<QATrendDTO> getQATrends();
    List<ProductionYieldDTO> getProductionYield();
    List<WorkOrderCycleDTO> getWorkOrderCycleTimes();
}