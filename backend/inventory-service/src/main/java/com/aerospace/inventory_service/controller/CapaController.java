package com.aerospace.inventory_service.controller;

import com.aerospace.inventory_service.common.ApiResponse;
import com.aerospace.inventory_service.dto.CapaOpenRequest;
import com.aerospace.inventory_service.dto.CapaResponse;
import com.aerospace.inventory_service.dto.CapaTransitionRequest;
import com.aerospace.inventory_service.service.CapaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('QA_INSPECTOR','MANAGER','ADMIN')")
public class CapaController {

    private final CapaService capaService;

    @PostMapping("/ncr/{ncrId}/capa")
    public ResponseEntity<ApiResponse<CapaResponse>> open(
            @PathVariable Long ncrId, @Valid @RequestBody CapaOpenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("CAPA_OPENED", capaService.openCapa(ncrId, request)));
    }

    @GetMapping("/ncr/{ncrId}/capa")
    public ResponseEntity<ApiResponse<List<CapaResponse>>> byNcr(@PathVariable Long ncrId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", capaService.getByNcr(ncrId)));
    }

    @GetMapping("/capa")
    public ResponseEntity<ApiResponse<List<CapaResponse>>> getAll(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", capaService.getAll(status)));
    }

    @GetMapping("/capa/{id}")
    public ResponseEntity<ApiResponse<CapaResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", capaService.getById(id)));
    }

    @PostMapping("/capa/{id}/root-cause")
    public ResponseEntity<ApiResponse<CapaResponse>> rootCause(@PathVariable Long id, @Valid @RequestBody CapaTransitionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("CAPA_ROOT_CAUSE_LOGGED", capaService.logRootCause(id, request)));
    }

    @PostMapping("/capa/{id}/action-plan")
    public ResponseEntity<ApiResponse<CapaResponse>> actionPlan(@PathVariable Long id, @Valid @RequestBody CapaTransitionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("CAPA_ACTION_PLANNED", capaService.planActions(id, request)));
    }

    @PostMapping("/capa/{id}/start")
    public ResponseEntity<ApiResponse<CapaResponse>> start(@PathVariable Long id, @Valid @RequestBody CapaTransitionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("CAPA_IN_PROGRESS", capaService.startImplementation(id, request)));
    }

    @PostMapping("/capa/{id}/submit-verification")
    public ResponseEntity<ApiResponse<CapaResponse>> submitVerification(@PathVariable Long id, @Valid @RequestBody CapaTransitionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("CAPA_PENDING_VERIFICATION", capaService.submitForVerification(id, request)));
    }

    @PostMapping("/capa/{id}/close")
    public ResponseEntity<ApiResponse<CapaResponse>> close(@PathVariable Long id, @Valid @RequestBody CapaTransitionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("CAPA_CLOSED", capaService.verifyAndClose(id, request)));
    }

    @PostMapping("/capa/{id}/reject")
    public ResponseEntity<ApiResponse<CapaResponse>> reject(@PathVariable Long id, @Valid @RequestBody CapaTransitionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("CAPA_REJECTED", capaService.reject(id, request)));
    }
}
