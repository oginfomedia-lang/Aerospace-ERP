package com.aerospace.procurement_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.procurement_service.dto.POApprovalActionRequest;
import com.aerospace.procurement_service.dto.POApprovalHistoryDTO;
import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;
import com.aerospace.procurement_service.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @Audited(action = "CREATE", entityType = "PurchaseOrder")
    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderResponse created = poService.createPurchaseOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<java.util.List<PurchaseOrderResponse>> getAllPurchaseOrders() {
        return ResponseEntity.ok(poService.getAllPurchaseOrders());
    }

    // Only works while status == DRAFT — this is how a PO with a bad SKU/supplier gets fixed
    @Audited(action = "UPDATE", entityType = "PurchaseOrder")
    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> updatePurchaseOrder(
            @PathVariable Long id, @Valid @RequestBody PurchaseOrderRequest request) {
        return ResponseEntity.ok(poService.updatePurchaseOrder(id, request));
    }

    // Only works for a DRAFT PO that was never submitted (no approval history)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseOrder(@PathVariable Long id) {
        poService.deletePurchaseOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<PurchaseOrderResponse> updatePOStatus(
            @PathVariable Long id,
            @RequestParam com.aerospace.procurement_service.entity.POStatus status) {
        return ResponseEntity.ok(poService.updatePOStatus(id, status));
    }

    // PUR-BR-001: kicks off (or auto-clears) the amount-based approval chain
    @Audited(action = "SUBMIT_FOR_APPROVAL", entityType = "PurchaseOrder")
    @PostMapping("/{id}/submit")
    public ResponseEntity<PurchaseOrderResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(poService.submitForApproval(id));
    }

    @Audited(action = "APPROVE", entityType = "PurchaseOrder")
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<PurchaseOrderResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) POApprovalActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(poService.approve(id,
                request != null ? request : new POApprovalActionRequest(),
                authentication.getName(), rolesOf(authentication)));
    }

    // PUR-BR-002: rejection remarks are mandatory — enforced in the service layer
    @Audited(action = "REJECT", entityType = "PurchaseOrder")
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<PurchaseOrderResponse> reject(
            @PathVariable Long id,
            @RequestBody POApprovalActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(poService.reject(id, request, authentication.getName(), rolesOf(authentication)));
    }

    @GetMapping("/{id}/approval-history")
    public ResponseEntity<List<POApprovalHistoryDTO>> getApprovalHistory(@PathVariable Long id) {
        return ResponseEntity.ok(poService.getApprovalHistory(id));
    }

    // Admin-only recovery tool for a PO left inconsistent by a past bug — recomputes
    // status/currentApprovalLevel from the PO's own approval history instead of a manual
    // SQL UPDATE. Deliberately not exposed to PROCUREMENT_OFFICER/MANAGER.
    @Audited(action = "RESYNC_APPROVAL_STATUS", entityType = "PurchaseOrder")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/resync-approval-status")
    public ResponseEntity<PurchaseOrderResponse> resyncApprovalStatus(@PathVariable Long id) {
        return ResponseEntity.ok(poService.resyncApprovalStatus(id));
    }

    private List<String> rolesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
    }
}
