package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.EmployeeReservationRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeReservationResponseDTO;
import com.aerospace.production_resource_service.service.EmployeeReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/resources/employee-reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','PRODUCTION_ENGINEER','MAINTENANCE_ENGINEER','MANAGER','ADMIN')")
public class EmployeeReservationController {

    private final EmployeeReservationService reservationService;

    @Audited(action = "CREATE", entityType = "EmployeeReservation")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeReservationResponseDTO>> createReservation(
            @Valid @RequestBody EmployeeReservationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("RESERVATION_CREATED", reservationService.createReservation(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeReservationResponseDTO>>> listReservations(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long productionOrderId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                reservationService.listReservations(employeeId, from, to, productionOrderId, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeReservationResponseDTO>> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", reservationService.getById(id)));
    }

    // Same purpose as MachineReservationController.checkActiveConflict — see there.
    @GetMapping("/active-conflict")
    public ResponseEntity<ApiResponse<EmployeeReservationResponseDTO>> checkActiveConflict(
            @RequestParam Long employeeId, @RequestParam Long excludeProductionOrderId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                reservationService.checkActiveConflict(employeeId, excludeProductionOrderId)));
    }

    @Audited(action = "UPDATE", entityType = "EmployeeReservation")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeReservationResponseDTO>> updateReservation(
            @PathVariable Long id, @Valid @RequestBody EmployeeReservationRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("RESERVATION_UPDATED", reservationService.updateReservation(id, dto)));
    }

    @Audited(action = "CANCEL", entityType = "EmployeeReservation")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.ok(new ApiResponse<>("RESERVATION_CANCELLED", null));
    }
}
