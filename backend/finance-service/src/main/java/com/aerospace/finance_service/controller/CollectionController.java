package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.CollectionActivityCreateDTO;
import com.aerospace.finance_service.dto.CollectionActivityDTO;
import com.aerospace.finance_service.service.CollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/ar/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<List<CollectionActivityDTO>> getAll() {
        return new ApiResponse<>("Collection activities fetched", collectionService.getAll());
    }

    @GetMapping("/customer/{customerRef}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<List<CollectionActivityDTO>> getByCustomer(@PathVariable String customerRef) {
        return new ApiResponse<>("Collection activities fetched",
                collectionService.getByCustomer(customerRef));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    @Audited(action = "LOG_ACTIVITY", entityType = "CollectionActivity")
    public ResponseEntity<ApiResponse<CollectionActivityDTO>> logActivity(
            @Valid @RequestBody CollectionActivityCreateDTO dto, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Collection activity logged",
                        collectionService.logActivity(dto, auth.getName())));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    @Audited(action = "RESOLVE", entityType = "CollectionActivity")
    public ApiResponse<CollectionActivityDTO> resolve(@PathVariable Long id) {
        return new ApiResponse<>("Collection activity resolved", collectionService.resolve(id));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN','MANAGER')")
    @Audited(action = "ESCALATE", entityType = "CollectionActivity")
    public ApiResponse<CollectionActivityDTO> escalate(@PathVariable Long id) {
        return new ApiResponse<>("Collection activity escalated", collectionService.escalate(id));
    }
}
