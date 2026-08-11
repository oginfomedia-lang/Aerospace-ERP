package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.MachineReservationRequestDTO;
import com.aerospace.production_resource_service.dto.MachineReservationResponseDTO;
import com.aerospace.production_resource_service.service.MachineReservationService;
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
@RequestMapping("/api/v1/resources/machine-reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','PRODUCTION_ENGINEER','MAINTENANCE_ENGINEER','MANAGER','ADMIN')")
public class MachineReservationController {

    private final MachineReservationService reservationService;

    @Audited(action = "CREATE", entityType = "MachineReservation")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<MachineReservationResponseDTO>> createReservation(
            @Valid @RequestBody MachineReservationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("RESERVATION_CREATED", reservationService.createReservation(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MachineReservationResponseDTO>>> listReservations(
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long productionOrderId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                reservationService.listReservations(machineId, from, to, productionOrderId, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineReservationResponseDTO>> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", reservationService.getById(id)));
    }

    // Called by production-service's startStage (forwarding the real caller's JWT) to
    // check whether starting this machine right now clashes with someone else's
    // advance booking. "data" is null when there's no conflict. Falls under the class
    // default read roles — same callers who are already allowed to occupy machines.
    @GetMapping("/active-conflict")
    public ResponseEntity<ApiResponse<MachineReservationResponseDTO>> checkActiveConflict(
            @RequestParam Long machineId, @RequestParam Long excludeProductionOrderId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                reservationService.checkActiveConflict(machineId, excludeProductionOrderId)));
    }

    @Audited(action = "UPDATE", entityType = "MachineReservation")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineReservationResponseDTO>> updateReservation(
            @PathVariable Long id, @Valid @RequestBody MachineReservationRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("RESERVATION_UPDATED", reservationService.updateReservation(id, dto)));
    }

    // Cancels (soft) rather than hard-deleting — see MachineReservationServiceImpl.
    @Audited(action = "CANCEL", entityType = "MachineReservation")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.ok(new ApiResponse<>("RESERVATION_CANCELLED", null));
    }
}
