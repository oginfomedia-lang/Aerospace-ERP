package com.aerospace.finance_service.controller;

import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.SupplierReferenceDTO;
import com.aerospace.finance_service.service.SupplierReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Read-only lookup — feeds the supplier dropdown on Journal Entries / Supplier
// Payments. No @PreAuthorize: matches CurrencyController's precedent that read-only
// reference lookups are open to any authenticated caller in this codebase.
@RestController
@RequestMapping("/api/v1/finance/suppliers")
@RequiredArgsConstructor
public class SupplierReferenceController {

    private final SupplierReferenceService supplierReferenceService;

    @GetMapping
    public ApiResponse<List<SupplierReferenceDTO>> getAll() {
        return new ApiResponse<>("Suppliers fetched", supplierReferenceService.getAll());
    }
}
