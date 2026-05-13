package com.aerospace.production_service.controller;

import com.aerospace.production_service.common.ApiResponse;
import com.aerospace.production_service.dto.*;
import com.aerospace.production_service.entity.ProductionOrderStatus;
import com.aerospace.production_service.service.ProductionOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RestController
@RequestMapping("/api/v1/production")
@RequiredArgsConstructor
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> create(
            @Valid @RequestBody ProductionOrderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(productionOrderService.createOrder(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductionOrderResponseDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(productionOrderService.getAll()));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<ProductionOrderResponseDTO>>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                productionOrderService.getAllPaged(PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productionOrderService.getById(id)));
    }

    @GetMapping("/po/{poNumber}")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> getByPoNumber(
            @PathVariable String poNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                productionOrderService.getByPoNumber(poNumber)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductionOrderRequestDTO dto) {
        return ResponseEntity.ok(ApiResponse.updated(
                productionOrderService.updateOrder(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productionOrderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    // ── SEARCH & FILTER ───────────────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductionOrderResponseDTO>>> search(
            @RequestParam String productName) {
        return ResponseEntity.ok(ApiResponse.success(
                productionOrderService.searchByProductName(productName)));
    }

    @GetMapping("/by-status")
    public ResponseEntity<ApiResponse<List<ProductionOrderResponseDTO>>> byStatus(
            @RequestParam ProductionOrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                productionOrderService.getByStatus(status)));
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<ProductionOrderResponseDTO>>> overdue() {
        return ResponseEntity.ok(ApiResponse.success(
                productionOrderService.getOverdueOrders()));
    }

    // ── LIFECYCLE TRANSITIONS ─────────────────────────────────────────────────

    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> release(
            @PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("ORDER_RELEASED",
                productionOrderService.releaseOrder(id)));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> start(
            @PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("PRODUCTION_STARTED",
                productionOrderService.startProduction(id)));
    }

    @PostMapping("/{id}/quality-check")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> moveToQA(
            @PathVariable Long id,
            @RequestParam(required = false) Integer producedQty,
            @RequestParam(required = false) Integer rejectedQty) {
        return ResponseEntity.ok(new ApiResponse<>("MOVED_TO_QUALITY_CHECK",
                productionOrderService.moveToQualityCheck(id, producedQty, rejectedQty)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> complete(
            @PathVariable Long id,
            @RequestParam Boolean qualityPassed,
            @RequestParam(required = false) String qualityNotes,
            @RequestParam(required = false) String closureNotes) {
        return ResponseEntity.ok(new ApiResponse<>("ORDER_COMPLETED",
                productionOrderService.completeOrder(id, qualityPassed,
                        qualityNotes, closureNotes)));
    }

    @PostMapping("/{id}/hold")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> hold(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(new ApiResponse<>("ORDER_ON_HOLD",
                productionOrderService.putOnHold(id, reason)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(new ApiResponse<>("ORDER_CANCELLED",
                productionOrderService.cancelOrder(id, reason)));
    }

    // ── MATERIAL MANAGEMENT ───────────────────────────────────────────────────

    @PostMapping("/{orderId}/materials/{materialId}/consume")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> consumeMaterial(
            @PathVariable Long orderId,
            @PathVariable Long materialId,
            @RequestParam int consumed,
            @RequestParam(defaultValue = "0") int wasted,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(new ApiResponse<>("MATERIAL_CONSUMED",
                productionOrderService.consumeMaterial(
                        orderId, materialId, consumed, wasted, remarks)));
    }

    @PostMapping("/{orderId}/materials/{materialId}/return")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> returnMaterial(
            @PathVariable Long orderId,
            @PathVariable Long materialId,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(new ApiResponse<>("MATERIAL_RETURNED",
                productionOrderService.returnMaterial(orderId, materialId, remarks)));
    }

    // ── STAGE LOGGING ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/stages")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> addStage(
            @PathVariable Long id,
            @Valid @RequestBody StageLogRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("STAGE_LOGGED",
                        productionOrderService.addStageLog(id, dto)));
    }
}
