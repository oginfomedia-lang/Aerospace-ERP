package com.aerospace.production_resource_service.controller;

import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.SkillUpgradeResponseDTO;
import com.aerospace.production_resource_service.service.SkillUpgradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// A top-level queue view across all employees — kept separate from
// SkillUpgradeRequestController since that one is nested under /employees/{employeeId}
// and Spring can't escape a class-level path prefix from a method mapping.
@RestController
@RequestMapping("/api/v1/resources/skill-upgrade-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
public class SkillUpgradePendingController {

    private final SkillUpgradeService skillUpgradeService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<SkillUpgradeResponseDTO>>> getPending() {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", skillUpgradeService.getPending()));
    }
}
