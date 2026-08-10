package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.EmployeeRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeResponseDTO;
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
}
