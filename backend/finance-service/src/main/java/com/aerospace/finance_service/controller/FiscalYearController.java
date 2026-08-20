package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.FiscalYearDTO;
import com.aerospace.finance_service.service.FiscalYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/fiscal-years")
@RequiredArgsConstructor
public class FiscalYearController {

    private final FiscalYearService fiscalYearService;

    @GetMapping
    public ApiResponse<List<FiscalYearDTO>> getAll() {
        return new ApiResponse<>("Fiscal years fetched", fiscalYearService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "FiscalYear")
    public ResponseEntity<ApiResponse<FiscalYearDTO>> create(@Valid @RequestBody FiscalYearDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Fiscal year created", fiscalYearService.create(dto)));
    }
}
