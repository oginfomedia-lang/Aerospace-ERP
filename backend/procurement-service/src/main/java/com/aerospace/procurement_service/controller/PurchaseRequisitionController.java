package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.dto.RequisitionDTOs.CreatePurchaseRequisitionRequest;
import com.aerospace.procurement_service.dto.RequisitionDTOs.PurchaseRequisitionResponse;
import com.aerospace.procurement_service.service.PurchaseRequisitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-requisitions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
public class PurchaseRequisitionController {

    private final PurchaseRequisitionService prService;

    @GetMapping
    public ResponseEntity<List<PurchaseRequisitionResponse>> getAll() {
        return ResponseEntity.ok(prService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseRequisitionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(prService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseRequisitionResponse> createManual(
            @Valid @RequestBody CreatePurchaseRequisitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prService.createManual(request));
    }
}