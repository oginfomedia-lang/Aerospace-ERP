package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.OrganizationDTO;
import com.aerospace.finance_service.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    public ApiResponse<List<OrganizationDTO>> getAll() {
        return new ApiResponse<>("Organizations fetched", organizationService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    public ApiResponse<OrganizationDTO> getById(@PathVariable Long id) {
        return new ApiResponse<>("Organization fetched", organizationService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "Organization")
    public ResponseEntity<ApiResponse<OrganizationDTO>> create(@Valid @RequestBody OrganizationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Organization created", organizationService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "UPDATE", entityType = "Organization")
    public ApiResponse<OrganizationDTO> update(@PathVariable Long id, @Valid @RequestBody OrganizationDTO dto) {
        return new ApiResponse<>("Organization updated", organizationService.update(id, dto));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "DEACTIVATE", entityType = "Organization")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        organizationService.deactivate(id);
        return new ApiResponse<>("Organization deactivated", null);
    }
}
