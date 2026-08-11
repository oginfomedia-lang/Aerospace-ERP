package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.EmployeeRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeResponseDTO;
import com.aerospace.production_resource_service.dto.OccupyRequestDTO;
import com.aerospace.production_resource_service.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','MANAGER','ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Audited(action = "CREATE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> registerEmployee(
            @Valid @RequestBody EmployeeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("EMPLOYEE_REGISTERED", employeeService.registerEmployee(dto)));
    }

    @Audited(action = "UPDATE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> updateEmployee(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("EMPLOYEE_UPDATED", employeeService.updateEmployee(id, dto)));
    }

    // PRODUCTION_ENGINEER needs read access to populate the Preferred Employee picker
    // in the Routing Template editor, without gaining any write access to the roster.
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> listEmployees(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long skillId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", employeeService.listEmployees(status, skillId)));
    }

    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", employeeService.getEmployeeById(id)));
    }

    // Record a skill certification/evaluation event — append-only, mirrors
    // MachineController.recordMaintenance's role split exactly.
    @Audited(action = "RECORD_SKILL_CERTIFICATION", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping("/{id}/skills")
    public ResponseEntity<ApiResponse<com.aerospace.production_resource_service.dto.EmployeeSkillCertificationResponseDTO>> recordSkillCertification(
            @PathVariable Long id, @Valid @RequestBody com.aerospace.production_resource_service.dto.EmployeeSkillCertificationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SKILL_CERTIFICATION_RECORDED", employeeService.recordSkillCertification(id, dto)));
    }

    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping("/{id}/skills")
    public ResponseEntity<ApiResponse<List<com.aerospace.production_resource_service.dto.EmployeeSkillCertificationResponseDTO>>> getSkillCertificationHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", employeeService.getSkillCertificationHistory(id)));
    }

    // Leave request/approve/reject/history now live in EmployeeLeaveController
    // (own nested resource at /employees/{employeeId}/leave/**), not here.

    // Called by production-service (forwarding the real caller's JWT) when a stage
    // actually starts using this employee — not intended to be called by a human directly.
    @Audited(action = "OCCUPY", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @PostMapping("/{id}/occupy")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> occupyEmployee(
            @PathVariable Long id, @RequestBody OccupyRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("EMPLOYEE_OCCUPIED", employeeService.occupyEmployee(id, dto.getStageLogId())));
    }

    // Called automatically when the stage using this employee finishes.
    @Audited(action = "RELEASE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> releaseEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("EMPLOYEE_RELEASED", employeeService.releaseEmployee(id)));
    }

    // Supervisor-only escape hatch for a stuck/forgotten release.
    @Audited(action = "FORCE_RELEASE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping("/{id}/force-release")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> forceReleaseEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("EMPLOYEE_RELEASED", employeeService.releaseEmployee(id)));
    }
}
