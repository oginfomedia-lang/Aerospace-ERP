package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.TaxJurisdictionDTO;
import com.aerospace.finance_service.service.TaxAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/tax-jurisdictions")
@RequiredArgsConstructor
public class TaxJurisdictionController {

    private final TaxAdminService taxAdminService;

    @GetMapping
    public ApiResponse<List<TaxJurisdictionDTO>> getAll() {
        return new ApiResponse<>("Tax jurisdictions fetched", taxAdminService.getAllJurisdictions());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "TaxJurisdiction")
    public ResponseEntity<ApiResponse<TaxJurisdictionDTO>> create(@Valid @RequestBody TaxJurisdictionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Tax jurisdiction created", taxAdminService.createJurisdiction(dto)));
    }
}
