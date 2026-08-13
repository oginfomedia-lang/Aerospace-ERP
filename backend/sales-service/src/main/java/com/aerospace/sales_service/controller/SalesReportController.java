package com.aerospace.sales_service.controller;

import com.aerospace.sales_service.common.ApiResponse;
import com.aerospace.sales_service.dto.SalesReportSummaryDTO;
import com.aerospace.sales_service.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES_MANAGER','MANAGER','ADMIN')")
public class SalesReportController {

    private final SalesReportService salesReportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SalesReportSummaryDTO>> summary(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", salesReportService.getSummary(days)));
    }
}