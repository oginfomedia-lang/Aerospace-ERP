package com.aerospace.procurement_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.procurement_service.dto.SupplierConfirmationRequest;
import com.aerospace.procurement_service.dto.SupplierRequest;
import com.aerospace.procurement_service.entity.Supplier;
import com.aerospace.procurement_service.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@Valid @RequestBody SupplierRequest request) {
        // Notice the @Valid annotation: It tells Spring to check the @NotBlank and
        // @Email rules in the DTO before hitting the service!
        Supplier created = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // QA_INSPECTOR needs read access to reach the page and confirm/reject — but not the
    // class-level create/update/delete roles, hence the override here rather than
    // widening the class-level @PreAuthorize.
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','QA_INSPECTOR','MANAGER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','QA_INSPECTOR','MANAGER','ADMIN')")
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, request));
    }

    // Soft delete — deactivates rather than removing, so PO/history references stay
    // intact
    @DeleteMapping("/{id}")
    public ResponseEntity<Supplier> deactivateSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.setActive(id, false));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Supplier> reactivateSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.setActive(id, true));
    }

    @Audited(action = "CONFIRM", entityType = "Supplier")
    @PreAuthorize("hasAnyRole('QA_INSPECTOR','MANAGER','ADMIN')")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Supplier> confirmSupplier(
            @PathVariable Long id,
            @RequestBody(required = false) SupplierConfirmationRequest request,
            Authentication authentication) {
        String remarks = request != null ? request.getRemarks() : null;
        return ResponseEntity.ok(supplierService.confirmSupplier(id, remarks, authentication.getName()));
    }

    @Audited(action = "REJECT", entityType = "Supplier")
    @PreAuthorize("hasAnyRole('QA_INSPECTOR','MANAGER','ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Supplier> rejectSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierConfirmationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(supplierService.rejectSupplier(id, request.getRemarks(), authentication.getName()));
    }
}
