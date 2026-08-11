package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.ShiftRequestDTO;
import com.aerospace.production_resource_service.dto.ShiftResponseDTO;
import com.aerospace.production_resource_service.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources/shifts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','MANAGER','ADMIN')")
public class ShiftController {

    private final ShiftService shiftService;

    @Audited(action = "CREATE", entityType = "Shift")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ShiftResponseDTO>> createShift(@Valid @RequestBody ShiftRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SHIFT_CREATED", shiftService.createShift(dto)));
    }

    // PRODUCTION_ENGINEER needs read access to see who's on shift when picking an
    // employee for a stage — same reasoning as Skill/Machine read access.
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShiftResponseDTO>>> listShifts() {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", shiftService.listShifts()));
    }

    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShiftResponseDTO>> getShift(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", shiftService.getShiftById(id)));
    }
}
