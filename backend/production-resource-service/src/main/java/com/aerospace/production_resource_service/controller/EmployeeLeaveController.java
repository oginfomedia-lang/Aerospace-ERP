package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.EmployeeLeaveRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeLeaveResponseDTO;
import com.aerospace.production_resource_service.dto.LeaveDecisionDTO;
import com.aerospace.production_resource_service.service.EmployeeLeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Own controller, nested under the Employee resource — same "record a
// sub-history-event" shape as EmployeeController's skill-certification endpoints,
// but a leave request is a single row moving PENDING -> APPROVED/REJECTED in place,
// not an append-only event log.
@RestController
@RequestMapping("/api/v1/resources/employees/{employeeId}/leave")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
public class EmployeeLeaveController {

    private final EmployeeLeaveService employeeLeaveService;

    @Audited(action = "REQUEST_LEAVE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeLeaveResponseDTO>> requestLeave(
            @PathVariable Long employeeId, @Valid @RequestBody EmployeeLeaveRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("LEAVE_REQUESTED", employeeLeaveService.requestLeave(employeeId, dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeLeaveResponseDTO>>> getLeaveHistory(@PathVariable Long employeeId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", employeeLeaveService.getLeaveHistory(employeeId)));
    }

    @Audited(action = "APPROVE_LEAVE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping("/{leaveId}/approve")
    public ResponseEntity<ApiResponse<EmployeeLeaveResponseDTO>> approveLeave(
            @PathVariable Long employeeId, @PathVariable Long leaveId, @Valid @RequestBody LeaveDecisionDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("LEAVE_APPROVED",
                employeeLeaveService.approveLeave(employeeId, leaveId, dto.getDecidedBy())));
    }

    @Audited(action = "REJECT_LEAVE", entityType = "Employee")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping("/{leaveId}/reject")
    public ResponseEntity<ApiResponse<EmployeeLeaveResponseDTO>> rejectLeave(
            @PathVariable Long employeeId, @PathVariable Long leaveId, @Valid @RequestBody LeaveDecisionDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("LEAVE_REJECTED",
                employeeLeaveService.rejectLeave(employeeId, leaveId, dto.getDecidedBy())));
    }
}
