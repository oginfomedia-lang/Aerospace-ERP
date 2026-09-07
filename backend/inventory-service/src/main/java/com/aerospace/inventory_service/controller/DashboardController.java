package com.aerospace.inventory_service.controller;

import com.aerospace.inventory_service.common.ApiResponse;
import com.aerospace.inventory_service.dto.DashboardResponseDTO;
import com.aerospace.inventory_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Internal-staff dashboard — a supplier account authenticates against this same
// gateway/JWT scheme, so without this guard a supplier login could pull the whole
// org's internal stock dashboard. Any staff role is fine; suppliers specifically aren't.
@PreAuthorize("!hasRole('SUPPLIER')")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ERP Dashboard: Real-time Stock Ledger & Balance overview
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardResponseDTO>> getDashboardStats() {
        DashboardResponseDTO stats = dashboardService.getDashboardStats();
        
        // This includes total approved stock, blocked QA stock, and rejected items
        return ResponseEntity.ok(new ApiResponse<>("ERP_STATS_RETRIEVED", stats));
    }
}
