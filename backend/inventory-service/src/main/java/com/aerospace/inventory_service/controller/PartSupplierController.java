package com.aerospace.inventory_service.controller;

import com.aerospace.inventory_service.common.ApiResponse;
import com.aerospace.inventory_service.dto.PartBasicDTO;
import com.aerospace.inventory_service.dto.PartSupplierRequest;
import com.aerospace.inventory_service.dto.PartSupplierResponse;
import com.aerospace.inventory_service.service.PartSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Approved Vendor List — which suppliers are approved to supply a given Part, and
// (reverse lookup) which parts a given supplier is approved to supply.
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class PartSupplierController {

    private final PartSupplierService partSupplierService;

    @GetMapping("/parts/{partId}/suppliers")
    public ResponseEntity<ApiResponse<List<PartSupplierResponse>>> getApprovedSuppliers(@PathVariable Long partId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", partSupplierService.getApprovedSuppliers(partId)));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
    @PostMapping("/parts/{partId}/suppliers")
    public ResponseEntity<ApiResponse<PartSupplierResponse>> addApprovedSupplier(
            @PathVariable Long partId, @RequestBody PartSupplierRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", partSupplierService.addApprovedSupplier(partId, request)));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
    @DeleteMapping("/parts/{partId}/suppliers/{supplierId}")
    public ResponseEntity<ApiResponse<String>> removeApprovedSupplier(@PathVariable Long partId, @PathVariable Long supplierId) {
        partSupplierService.removeApprovedSupplier(partId, supplierId);
        return ResponseEntity.ok(new ApiResponse<>("REMOVED", "Supplier removed from the approved vendor list"));
    }

    @GetMapping("/suppliers/{supplierId}/parts")
    public ResponseEntity<ApiResponse<List<PartBasicDTO>>> getPartsForSupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", partSupplierService.getPartsForSupplier(supplierId)));
    }
}
