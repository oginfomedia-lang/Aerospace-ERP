package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.*;
import com.aerospace.production_resource_service.service.MachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources/machines")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','MANAGER','ADMIN')")
public class MachineController {

    private final MachineService machineService;

    // Registering a new machine is deliberately narrower than the class default —
    // a maintenance engineer services machines, they don't add new ones to the registry.
    @Audited(action = "CREATE", entityType = "Machine")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<MachineResponseDTO>> registerMachine(
            @Valid @RequestBody MachineRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("MACHINE_REGISTERED", machineService.registerMachine(dto)));
    }

    // PRODUCTION_ENGINEER needs read access to populate the Preferred Machine picker
    // in the Routing Template editor, without gaining any write access to the registry.
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MachineResponseDTO>>> listMachines(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", machineService.listMachines(status)));
    }

    @GetMapping("/due")
    public ResponseEntity<ApiResponse<List<MachineResponseDTO>>> getDueList(
            @RequestParam(defaultValue = "30") int withinDays) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", machineService.getDueList(withinDays)));
    }

    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineResponseDTO>> getMachine(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", machineService.getMachineById(id)));
    }

    @Audited(action = "RECORD_MAINTENANCE", entityType = "Machine")
    @PostMapping("/{id}/maintenance")
    public ResponseEntity<ApiResponse<MaintenanceRecordResponseDTO>> recordMaintenance(
            @PathVariable Long id, @Valid @RequestBody MaintenanceRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("MAINTENANCE_RECORDED", machineService.recordMaintenance(id, dto)));
    }

    @GetMapping("/{id}/maintenance/history")
    public ResponseEntity<ApiResponse<List<MaintenanceRecordResponseDTO>>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", machineService.getMaintenanceHistory(id)));
    }
}