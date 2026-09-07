package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.TaxCategoryDTO;
import com.aerospace.finance_service.service.TaxAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/tax-categories")
@RequiredArgsConstructor
public class TaxCategoryController {

    private final TaxAdminService taxAdminService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    public ApiResponse<List<TaxCategoryDTO>> getAll() {
        return new ApiResponse<>("Tax categories fetched", taxAdminService.getAllCategories());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "TaxCategory")
    public ResponseEntity<ApiResponse<TaxCategoryDTO>> create(@Valid @RequestBody TaxCategoryDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Tax category created", taxAdminService.createCategory(dto)));
    }
}
