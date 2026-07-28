package com.aerospace.production_service.controller;

import com.aerospace.production_service.common.ApiResponse;
import com.aerospace.production_service.dto.RoutingTemplateRequestDTO;
import com.aerospace.production_service.dto.RoutingTemplateResponseDTO;
import com.aerospace.production_service.service.RoutingTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RestController
@RequestMapping("/api/v1/production/routing-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_ENGINEER','MANAGER','ADMIN')")
public class RoutingTemplateController {

    private final RoutingTemplateService service;

    @PostMapping
    public ResponseEntity<ApiResponse<RoutingTemplateResponseDTO>> create(
            @Valid @RequestBody RoutingTemplateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("CREATED", service.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoutingTemplateResponseDTO>>> getAll(
            @RequestParam(required = false) String partNumber) {
        List<RoutingTemplateResponseDTO> result = partNumber != null
                ? service.getByPartNumber(partNumber)
                : service.getAll();
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoutingTemplateResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", service.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoutingTemplateResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody RoutingTemplateRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("UPDATED", service.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("DELETED", null));
    }
}
