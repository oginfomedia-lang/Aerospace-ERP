package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.PlantDTO;
import com.aerospace.finance_service.service.PlantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    @GetMapping
    public ApiResponse<List<PlantDTO>> getAll() {
        return new ApiResponse<>("Plants fetched", plantService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PlantDTO> getById(@PathVariable Long id) {
        return new ApiResponse<>("Plant fetched", plantService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "Plant")
    public ResponseEntity<ApiResponse<PlantDTO>> create(@Valid @RequestBody PlantDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Plant created", plantService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "UPDATE", entityType = "Plant")
    public ApiResponse<PlantDTO> update(@PathVariable Long id, @Valid @RequestBody PlantDTO dto) {
        return new ApiResponse<>("Plant updated", plantService.update(id, dto));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "DEACTIVATE", entityType = "Plant")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        plantService.deactivate(id);
        return new ApiResponse<>("Plant deactivated", null);
    }
}
