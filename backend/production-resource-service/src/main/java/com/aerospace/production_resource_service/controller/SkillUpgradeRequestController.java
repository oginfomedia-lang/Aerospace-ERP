package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.LeaveDecisionDTO;
import com.aerospace.production_resource_service.dto.SkillUpgradeRequestCreateDTO;
import com.aerospace.production_resource_service.dto.SkillUpgradeResponseDTO;
import com.aerospace.production_resource_service.service.SkillUpgradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Own controller nested under the Employee resource — same "single-lifecycle request"
// shape as EmployeeLeaveController (PENDING -> APPROVED/REJECTED in place).
@RestController
@RequestMapping("/api/v1/resources/employees/{employeeId}/skill-upgrade-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
public class SkillUpgradeRequestController {

    private final SkillUpgradeService skillUpgradeService;

    @Audited(action = "REQUEST_SKILL_UPGRADE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<SkillUpgradeResponseDTO>> requestUpgrade(
            @PathVariable Long employeeId, @Valid @RequestBody SkillUpgradeRequestCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SKILL_UPGRADE_REQUESTED", skillUpgradeService.requestUpgrade(employeeId, dto, dto.getRequestedBy())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillUpgradeResponseDTO>>> getHistory(@PathVariable Long employeeId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", skillUpgradeService.getHistory(employeeId)));
    }

    @Audited(action = "APPROVE_SKILL_UPGRADE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<SkillUpgradeResponseDTO>> approve(
            @PathVariable Long employeeId, @PathVariable Long requestId, @Valid @RequestBody LeaveDecisionDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("SKILL_UPGRADE_APPROVED",
                skillUpgradeService.approve(employeeId, requestId, dto.getDecidedBy())));
    }

    @Audited(action = "REJECT_SKILL_UPGRADE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<SkillUpgradeResponseDTO>> reject(
            @PathVariable Long employeeId, @PathVariable Long requestId, @Valid @RequestBody LeaveDecisionDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("SKILL_UPGRADE_REJECTED",
                skillUpgradeService.reject(employeeId, requestId, dto.getDecidedBy())));
    }
}
