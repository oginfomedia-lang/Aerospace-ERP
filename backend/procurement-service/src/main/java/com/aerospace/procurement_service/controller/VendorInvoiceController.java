package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.dto.InvoiceActionRequest;
import com.aerospace.procurement_service.dto.VendorInvoiceRequest;
import com.aerospace.procurement_service.dto.VendorInvoiceResponse;
import com.aerospace.procurement_service.service.VendorInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Three-way match: vendor invoices matched against the PO's negotiated price and the
// QA-accepted quantity already on POLineItem.
@RestController
@RequestMapping("/api/vendor-invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
public class VendorInvoiceController {

    private final VendorInvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<VendorInvoiceResponse> create(@Valid @RequestBody VendorInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createAndMatch(request));
    }

    @GetMapping
    public ResponseEntity<List<VendorInvoiceResponse>> getAll() {
        return ResponseEntity.ok(invoiceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorInvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<VendorInvoiceResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) InvoiceActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(invoiceService.approve(id,
                request != null ? request : new InvoiceActionRequest(), authentication.getName()));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<VendorInvoiceResponse> reject(
            @PathVariable Long id,
            @RequestBody InvoiceActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(invoiceService.reject(id, request, authentication.getName()));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<VendorInvoiceResponse> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.markPaid(id));
    }
}
