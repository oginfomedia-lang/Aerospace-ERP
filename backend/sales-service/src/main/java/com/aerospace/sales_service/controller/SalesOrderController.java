package com.aerospace.sales_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.sales_service.common.ApiResponse;
import com.aerospace.sales_service.dto.ApprovalDecisionDTO;
import com.aerospace.sales_service.dto.AtpResultDTO;
import com.aerospace.sales_service.dto.OrderTrackingDTO;
import com.aerospace.sales_service.dto.SalesOrderRequestDTO;
import com.aerospace.sales_service.dto.SalesOrderResponseDTO;
import com.aerospace.sales_service.service.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/sales/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES_EXECUTIVE','SALES_MANAGER','MANAGER','ADMIN')")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @Audited(action = "CREATE", entityType = "SalesOrder")
    @PostMapping
    public ResponseEntity<ApiResponse<SalesOrderResponseDTO>> create(
            @Valid @RequestBody SalesOrderRequestDTO dto, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SALES_ORDER_CREATED", salesOrderService.createOrder(dto, auth.getName())));
    }

    @Audited(action = "UPDATE", entityType = "SalesOrder")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesOrderResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody SalesOrderRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("SALES_ORDER_UPDATED", salesOrderService.updateOrder(id, dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDTO>>> list(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", salesOrderService.listOrders(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesOrderResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", salesOrderService.getById(id)));
    }

    // Every revision of this order's number, newest first — the regression check for
    // the versioning design: an old revision must still resolve even after a new one
    // supersedes it.
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDTO>>> history(@PathVariable Long id) {
        String orderNumber = salesOrderService.getById(id).getOrderNumber();
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", salesOrderService.getHistory(orderNumber)));
    }

    @Audited(action = "SUBMIT_FOR_APPROVAL", entityType = "SalesOrder")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<SalesOrderResponseDTO>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUBMITTED", salesOrderService.submitForApproval(id)));
    }

    @Audited(action = "APPROVE", entityType = "SalesOrder")
    @PreAuthorize("hasAnyRole('SALES_MANAGER','MANAGER','ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<SalesOrderResponseDTO>> approve(
            @PathVariable Long id, @RequestBody(required = false) ApprovalDecisionDTO dto, Authentication auth) {
        String remarks = dto != null ? dto.getRemarks() : null;
        return ResponseEntity.ok(new ApiResponse<>("APPROVED",
                salesOrderService.approve(id, auth.getName(), rolesOf(auth), remarks)));
    }

    @Audited(action = "REJECT", entityType = "SalesOrder")
    @PreAuthorize("hasAnyRole('SALES_MANAGER','MANAGER','ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<SalesOrderResponseDTO>> reject(
            @PathVariable Long id, @RequestBody(required = false) ApprovalDecisionDTO dto, Authentication auth) {
        String remarks = dto != null ? dto.getRemarks() : null;
        return ResponseEntity.ok(new ApiResponse<>("REJECTED",
                salesOrderService.reject(id, auth.getName(), rolesOf(auth), remarks)));
    }

    // §18.8: "can we ship this now, and if not, when can we?" — runs ATP for every
    // line, and (as a side effect) sets the order's promisedDeliveryDate.
    @GetMapping("/{id}/atp")
    public ResponseEntity<ApiResponse<List<AtpResultDTO>>> evaluateAtp(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", salesOrderService.evaluateAtp(id)));
    }

    // §18.9: reserves real stock, batch by batch, against a CONFIRMED order.
    @Audited(action = "ALLOCATE", entityType = "SalesOrder")
    @PostMapping("/{id}/allocate")
    public ResponseEntity<ApiResponse<SalesOrderResponseDTO>> allocate(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("ALLOCATED", salesOrderService.allocateOrder(id)));
    }

    // §18.12: which batch, which shipment — a read-only rollup, no cross-service calls.
    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingDTO>> tracking(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", salesOrderService.getTracking(id)));
    }

    // Authentication's roles come back prefixed "ROLE_" (that's how @PreAuthorize
    // matches them) — ApprovalEngine wants the bare role name, so strip it here, the
    // same way PurchaseOrderController.rolesOf() already does.
    private Set<String> rolesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toSet());
    }

    @Audited(action = "RELEASE_ALLOCATION", entityType = "SalesOrder")
    @PreAuthorize("hasAnyRole('SALES_MANAGER','WAREHOUSE_OPERATOR','MANAGER','ADMIN')")
    @PostMapping("/{id}/release-allocation")
    public ResponseEntity<ApiResponse<SalesOrderResponseDTO>> releaseAllocation(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("ALLOCATION_RELEASED", salesOrderService.releaseAllocation(id)));
    }
}