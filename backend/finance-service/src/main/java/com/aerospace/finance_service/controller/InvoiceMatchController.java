package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.InvoiceMatchDTO;
import com.aerospace.finance_service.dto.InvoiceMatchRequestDTO;
import com.aerospace.finance_service.service.InvoiceMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/ap/invoice-match")
@RequiredArgsConstructor
public class InvoiceMatchController {

    private final InvoiceMatchService invoiceMatchService;

    @GetMapping
    public ApiResponse<List<InvoiceMatchDTO>> getAll() {
        return new ApiResponse<>("Invoice match records fetched", invoiceMatchService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceMatchDTO> getById(@PathVariable Long id) {
        return new ApiResponse<>("Invoice match record fetched", invoiceMatchService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    @Audited(action = "INVOICE_MATCH", entityType = "InvoiceMatch")
    public ResponseEntity<ApiResponse<InvoiceMatchDTO>> match(
            @Valid @RequestBody InvoiceMatchRequestDTO request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("3-way match completed",
                        invoiceMatchService.match(request, auth.getName())));
    }
}
