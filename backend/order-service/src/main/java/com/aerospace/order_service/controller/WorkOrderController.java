package com.aerospace.order_service.controller;

import com.aerospace.order_service.dto.ApiResponse;
import com.aerospace.order_service.dto.WorkOrderDTOs.*;
import com.aerospace.order_service.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/work-orders")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('PRODUCTION_ENGINEER','MANAGER','ADMIN')")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/work-orders
     * Create a new work order with optional material line items.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderResponse>> create(
            @Valid @RequestBody CreateWorkOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Work order created", workOrderService.createWorkOrder(request)));
    }

    /**
     * GET /api/v1/work-orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getAll()));
    }

    /**
     * GET /api/v1/work-orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getById(id)));
    }

    /**
     * GET /api/v1/work-orders/number/{woNumber}
     */
    @GetMapping("/number/{woNumber}")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> getByWoNumber(
            @PathVariable String woNumber) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getByWoNumber(woNumber)));
    }

    /**
     * PUT /api/v1/work-orders/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> update(
            @PathVariable Long id,
            @RequestBody UpdateWorkOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.updateWorkOrder(id, request)));
    }

    /**
     * DELETE /api/v1/work-orders/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        workOrderService.deleteWorkOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Work order deleted", null));
    }

    // ── Status Lifecycle ──────────────────────────────────────────────────────

    /**
     * POST /api/v1/work-orders/{id}/transition
     * Body: { "newStatus": "OPEN", "changedBy": "J. Smith", "remarks": "..." }
     * Valid transitions:
     *   DRAFT → OPEN | CANCELLED
     *   OPEN  → IN_PROGRESS | ON_HOLD | CANCELLED
     *   IN_PROGRESS → ON_HOLD | COMPLETED | CANCELLED
     *   ON_HOLD → IN_PROGRESS | CANCELLED
     */
    @PostMapping("/{id}/transition")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> transition(
            @PathVariable Long id,
            @Valid @RequestBody StatusTransitionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.transitionStatus(id, request)));
    }

    // ── Material Management ───────────────────────────────────────────────────

    /**
     * POST /api/v1/work-orders/{id}/material-items
     * Add a material line item to an existing WO.
     */
    @PostMapping("/{id}/material-items")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> addLineItem(
            @PathVariable Long id,
            @Valid @RequestBody MaterialLineItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.addMaterialLineItem(id, request)));
    }

    /**
     * POST /api/v1/work-orders/{id}/issue-material
     * Mark a line item as issued (after inventory has issued the stock).
     * Body: { "lineItemId": 3, "quantityIssued": 5, "batchNumber": "BTH-001", "issuedBy": "..." }
     */
    @PostMapping("/{id}/issue-material")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> issueMaterial(
            @PathVariable Long id,
            @Valid @RequestBody IssueMaterialRequest request) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.issueMaterial(id, request)));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/work-orders/status/{status}
     * status: DRAFT | OPEN | IN_PROGRESS | ON_HOLD | COMPLETED | CANCELLED
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> getByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getByStatus(status)));
    }

    /**
     * GET /api/v1/work-orders/priority/{priority}
     * priority: LOW | MEDIUM | HIGH | CRITICAL
     */
    @GetMapping("/priority/{priority}")
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> getByPriority(
            @PathVariable String priority) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getByPriority(priority)));
    }

    /**
     * GET /api/v1/work-orders/aircraft/{aircraftId}
     */
    @GetMapping("/aircraft/{aircraftId}")
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> getByAircraft(
            @PathVariable String aircraftId) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getByAircraft(aircraftId)));
    }

    /**
     * GET /api/v1/work-orders/overdue
     */
    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> getOverdue() {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getOverdue()));
    }

    /**
     * GET /api/v1/work-orders/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> getRecent() {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getRecent()));
    }

    /**
     * GET /api/v1/work-orders/search?q=Boeing
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.search(q)));
    }

    /**
     * GET /api/v1/work-orders/needing-part/{partId}
     * Open (not COMPLETED/CANCELLED) material lines for this part, still PENDING or
     * PARTIALLY_ISSUED — used to let Inventory's ad-hoc Issue flow attribute an issuance
     * to a real Work Order instead of a free-text purpose.
     */
    @GetMapping("/needing-part/{partId}")
    public ResponseEntity<ApiResponse<List<WorkOrderNeedingPartResponse>>> needingPart(
            @PathVariable Long partId) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.findWorkOrdersNeedingPart(partId)));
    }

    /**
     * GET /api/v1/work-orders/{id}/history
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<StatusHistoryResponse>>> getHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getStatusHistory(id)));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/work-orders/dashboard/stats
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getDashboardStats()));
    }

    /**
     * GET /api/v1/work-orders/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("work-order-service UP"));
    }
}