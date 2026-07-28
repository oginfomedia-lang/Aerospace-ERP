package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.audit.Audited;
import com.aerospace.procurement_service.dto.RequisitionDTOs.CreatePurchaseRequisitionRequest;
import com.aerospace.procurement_service.dto.RequisitionDTOs.PrApprovalActionRequest;
import com.aerospace.procurement_service.dto.RequisitionDTOs.PurchaseRequisitionResponse;
import com.aerospace.procurement_service.service.PurchaseRequisitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @Audited(action = "CREATE", entityType = "PurchaseRequisition")
    @PostMapping
    public ResponseEntity<PurchaseRequisitionResponse> createManual(
            @Valid @RequestBody CreatePurchaseRequisitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prService.createManual(request));
    }

    // Approval is a step above the officer who raised the PR — same reasoning as PO award (PUR-BR-001)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Audited(action = "APPROVE", entityType = "PurchaseRequisition")
    @PostMapping("/{id}/approve")
    public ResponseEntity<PurchaseRequisitionResponse> approve(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(prService.approve(id, authentication.getName()));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Audited(action = "REJECT", entityType = "PurchaseRequisition")
    @PostMapping("/{id}/reject")
    public ResponseEntity<PurchaseRequisitionResponse> reject(
            @PathVariable Long id, @RequestBody PrApprovalActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(prService.reject(id, authentication.getName(), request.getRemarks()));
    }
}
