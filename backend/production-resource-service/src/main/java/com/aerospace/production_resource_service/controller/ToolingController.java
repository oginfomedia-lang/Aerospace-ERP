package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.OccupyRequestDTO;
import com.aerospace.production_resource_service.dto.ToolingRequestDTO;
import com.aerospace.production_resource_service.dto.ToolingResponseDTO;
import com.aerospace.production_resource_service.service.ToolingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources/tooling")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','MANAGER','ADMIN')")
public class ToolingController {

    private final ToolingService toolingService;

    // Registering a new tool is narrower than the class default, same reasoning as
    // MachineController.registerMachine — servicing a tool isn't the same job as
    // adding one to the registry.
    @Audited(action = "CREATE", entityType = "Tooling")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ToolingResponseDTO>> registerTooling(@Valid @RequestBody ToolingRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("TOOLING_REGISTERED", toolingService.registerTooling(dto)));
    }

    @Audited(action = "UPDATE", entityType = "Tooling")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ToolingResponseDTO>> updateTooling(
            @PathVariable Long id, @Valid @RequestBody ToolingRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("TOOLING_UPDATED", toolingService.updateTooling(id, dto)));
    }

    @Audited(action = "DELETE", entityType = "Tooling")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTooling(@PathVariable Long id) {
        toolingService.deleteTooling(id);
        return ResponseEntity.ok(new ApiResponse<>("TOOLING_DELETED", null));
    }

    // PRODUCTION_ENGINEER needs read access to populate the Required Tooling picker
    // in the Routing Template editor, without gaining any write access to the registry.
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ToolingResponseDTO>>> listTooling(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String toolingType) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", toolingService.listTooling(status, toolingType)));
    }

    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ToolingResponseDTO>> getTooling(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", toolingService.getToolingById(id)));
    }

    // Called by production-service (forwarding the real caller's JWT) when a stage
    // actually starts using this tool — not intended to be called by a human directly.
    @Audited(action = "OCCUPY", entityType = "Tooling")
    @PreAuthorize("hasAnyRole('PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @PostMapping("/{id}/occupy")
    public ResponseEntity<ApiResponse<ToolingResponseDTO>> occupyTooling(
            @PathVariable Long id, @RequestBody OccupyRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("TOOLING_OCCUPIED", toolingService.occupyTooling(id, dto.getStageLogId())));
    }

    // Called automatically when the stage using this tool finishes.
    @Audited(action = "RELEASE", entityType = "Tooling")
    @PreAuthorize("hasAnyRole('PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<ToolingResponseDTO>> releaseTooling(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("TOOLING_RELEASED", toolingService.releaseTooling(id)));
    }

    // Supervisor-only escape hatch for a stuck/forgotten release.
    @Audited(action = "FORCE_RELEASE", entityType = "Tooling")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping("/{id}/force-release")
    public ResponseEntity<ApiResponse<ToolingResponseDTO>> forceReleaseTooling(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("TOOLING_RELEASED", toolingService.releaseTooling(id)));
    }
}
