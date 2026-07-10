package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.dto.SupplierRequest;
import com.aerospace.procurement_service.entity.Supplier;
import com.aerospace.procurement_service.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@Valid @RequestBody SupplierRequest request) {
        // Notice the @Valid annotation: It tells Spring to check the @NotBlank and @Email rules in the DTO before hitting the service!
        Supplier created = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, request));
    }

    // Soft delete — deactivates rather than removing, so PO/history references stay intact
    @DeleteMapping("/{id}")
    public ResponseEntity<Supplier> deactivateSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.setActive(id, false));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Supplier> reactivateSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.setActive(id, true));
    }
}
