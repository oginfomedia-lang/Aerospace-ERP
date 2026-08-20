package com.aerospace.finance_service.controller;

import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.CustomerReferenceDTO;
import com.aerospace.finance_service.service.CustomerReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Read-only lookup — feeds the customer dropdown on Journal Entries / customer
// invoice posting. No @PreAuthorize, matching SupplierReferenceController's precedent.
@RestController
@RequestMapping("/api/v1/finance/customers")
@RequiredArgsConstructor
public class CustomerReferenceController {

    private final CustomerReferenceService customerReferenceService;

    @GetMapping
    public ApiResponse<List<CustomerReferenceDTO>> getAll() {
        return new ApiResponse<>("Customers fetched", customerReferenceService.getAll());
    }
}
