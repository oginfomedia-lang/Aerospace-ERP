package com.aerospace.production_service.controller;

import com.aerospace.production_service.common.ApiResponse;
import com.aerospace.production_service.dto.DashboardResponseDTO;
import com.aerospace.production_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RestController
@RequestMapping("/api/v1/production/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardResponseDTO>> getStats() {
        DashboardResponseDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(new ApiResponse<>("PRODUCTION_STATS_RETRIEVED", stats));
    }
}
