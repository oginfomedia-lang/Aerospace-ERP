package com.aerospace.reporting_service.controller;

import com.aerospace.reporting_service.client.dto.SalesReportSummaryDTO;
import com.aerospace.reporting_service.dto.*;
import com.aerospace.reporting_service.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("!hasRole('SUPPLIER')")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDashboardSummary()));
    }

    @GetMapping("/inventory/value")
    public ResponseEntity<ApiResponse<List<InventoryValueDTO>>> getInventoryValue() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getInventoryValueByPart()));
    }

    @GetMapping("/qa/trends")
    public ResponseEntity<ApiResponse<List<QATrendDTO>>> getQATrends() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getQATrends()));
    }

    @GetMapping("/production/yield")
    public ResponseEntity<ApiResponse<List<ProductionYieldDTO>>> getProductionYield() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getProductionYield()));
    }

    @GetMapping("/work-orders/cycle-times")
    public ResponseEntity<ApiResponse<List<WorkOrderCycleDTO>>> getWorkOrderCycleTimes() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getWorkOrderCycleTimes()));
    }

    @GetMapping("/sales/summary")
    public ResponseEntity<ApiResponse<SalesReportSummaryDTO>> getSalesSummary (@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSalesSummary(days)));
    }
}