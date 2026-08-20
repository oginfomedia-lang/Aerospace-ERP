package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.TaxRateDTO;
import com.aerospace.finance_service.service.TaxAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/tax-rates")
@RequiredArgsConstructor
public class TaxRateController {

    private final TaxAdminService taxAdminService;

    @GetMapping
    public ApiResponse<List<TaxRateDTO>> getAll() {
        return new ApiResponse<>("Tax rates fetched", taxAdminService.getAllRates());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "TaxRate")
    public ResponseEntity<ApiResponse<TaxRateDTO>> create(@Valid @RequestBody TaxRateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Tax rate created", taxAdminService.createRate(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "UPDATE", entityType = "TaxRate")
    public ApiResponse<TaxRateDTO> update(@PathVariable Long id, @Valid @RequestBody TaxRateDTO dto) {
        return new ApiResponse<>("Tax rate updated", taxAdminService.updateRate(id, dto));
    }
}
