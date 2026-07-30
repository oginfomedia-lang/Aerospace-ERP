package com.aerospace.inventory_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.inventory_service.common.ApiResponse;
import com.aerospace.inventory_service.dto.FormGrnRequest;
import com.aerospace.inventory_service.dto.MaterialReceiptRequest;
import com.aerospace.inventory_service.dto.MaterialReceiptResponse;
import com.aerospace.inventory_service.service.MaterialReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Step 1 of the gate-to-GRN flow: goods are logged here at the physical gate/dock
// BEFORE any GRN or QC bucket exists. GRN is only ever formed from an existing
// Material Receipt via /form-grn — never directly from a Purchase Order.
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/inventory/material-receipts")
@RequiredArgsConstructor
public class MaterialReceiptController {

    private final MaterialReceiptService materialReceiptService;

    @Audited(action = "CREATE", entityType = "MaterialReceipt")
    @PreAuthorize("hasAnyRole('STORE_KEEPER','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<MaterialReceiptResponse>> create(@RequestBody MaterialReceiptRequest request) {
        MaterialReceiptResponse response = materialReceiptService.createReceipt(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("RECEIVED_AT_GATE", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaterialReceiptResponse>>> getAll(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", materialReceiptService.getAll(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialReceiptResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", materialReceiptService.getById(id)));
    }

    @Audited(action = "FORM_GRN", entityType = "MaterialReceipt")
    @PreAuthorize("hasAnyRole('STORE_KEEPER','MANAGER','ADMIN')")
    @PostMapping("/{id}/form-grn")
    public ResponseEntity<ApiResponse<MaterialReceiptResponse>> formGrn(
            @PathVariable Long id, @RequestBody FormGrnRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("GRN_FORMED", materialReceiptService.formGrn(id, request)));
    }
}
