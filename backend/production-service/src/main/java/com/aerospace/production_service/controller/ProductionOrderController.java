package com.aerospace.production_service.controller;

import com.aerospace.common.audit.Audited;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RestController
@RequestMapping("/api/v1/production")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_ENGINEER','MANAGER','ADMIN')")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Audited(action = "CREATE", entityType = "ProductionOrder")
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

    @Audited(action = "UPDATE", entityType = "ProductionOrder")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductionOrderRequestDTO dto) {
        return ResponseEntity.ok(ApiResponse.updated(
                productionOrderService.updateOrder(id, dto)));
    }

    @Audited(action = "DELETE", entityType = "ProductionOrder")
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

    // WO<->Production visibility — lets the Work Order UI show a link/badge to
    // whatever ProductionOrder(s) were auto-created from it.
    @GetMapping("/by-work-order/{workOrderId}")
    public ResponseEntity<ApiResponse<List<ProductionOrderResponseDTO>>> byWorkOrder(
            @PathVariable Long workOrderId) {
        return ResponseEntity.ok(ApiResponse.success(
                productionOrderService.getByWorkOrderId(workOrderId)));
    }

    // Open standalone (no Work Order) materials still needing this part — used to let
    // Inventory's ad-hoc Issue flow attribute an issuance to a real Production Order
    // instead of a free-text purpose. WO-derived materials are excluded here since
    // they're already reachable via the Work Order's own needing-part endpoint.
    @GetMapping("/needing-part/{partId}")
    public ResponseEntity<ApiResponse<List<ProductionOrderNeedingPartResponse>>> needingPart(
            @PathVariable Long partId) {
        return ResponseEntity.ok(ApiResponse.success(
                productionOrderService.findProductionOrdersNeedingPart(partId)));
    }

    // ── LIFECYCLE TRANSITIONS ─────────────────────────────────────────────────

    @Audited(action = "RELEASE", entityType = "ProductionOrder")
    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> release(
            @PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("ORDER_RELEASED",
                productionOrderService.releaseOrder(id)));
    }

    @Audited(action = "START", entityType = "ProductionOrder")
    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> start(
            @PathVariable Long id,
            @RequestParam(required = false) List<Long> skipRoutingForMaterialIds) {
        return ResponseEntity.ok(new ApiResponse<>("PRODUCTION_STARTED",
                productionOrderService.startProduction(id, skipRoutingForMaterialIds)));
    }

    @Audited(action = "START_ASSEMBLY", entityType = "ProductionOrder")
    @PostMapping("/{id}/start-assembly")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> startAssembly(
            @PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("ASSEMBLY_STARTED",
                productionOrderService.startAssembly(id)));
    }

    @Audited(action = "QUALITY_CHECK", entityType = "ProductionOrder")
    @PostMapping("/{id}/quality-check")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> moveToQA(
            @PathVariable Long id,
            @RequestParam(required = false) Integer producedQty,
            @RequestParam(required = false) Integer rejectedQty) {
        return ResponseEntity.ok(new ApiResponse<>("MOVED_TO_QUALITY_CHECK",
                productionOrderService.moveToQualityCheck(id, producedQty, rejectedQty)));
    }

    @Audited(action = "COMPLETE", entityType = "ProductionOrder")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> complete(
            @PathVariable Long id,
            @RequestParam Boolean qualityPassed,
            @RequestParam(required = false) String qualityNotes,
            @RequestParam(required = false) String closureNotes,
            @RequestParam(required = false) String outputLocationCode) {
        return ResponseEntity.ok(new ApiResponse<>("ORDER_COMPLETED",
                productionOrderService.completeOrder(id, qualityPassed,
                        qualityNotes, closureNotes, outputLocationCode)));
    }

    @Audited(action = "HOLD", entityType = "ProductionOrder")
    @PostMapping("/{id}/hold")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> hold(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(new ApiResponse<>("ORDER_ON_HOLD",
                productionOrderService.putOnHold(id, reason)));
    }

    @Audited(action = "CANCEL", entityType = "ProductionOrder")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(new ApiResponse<>("ORDER_CANCELLED",
                productionOrderService.cancelOrder(id, reason)));
    }

    // ── MATERIAL MANAGEMENT ───────────────────────────────────────────────────

    @Audited(action = "CONSUME_MATERIAL", entityType = "ProductionOrder")
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

    @Audited(action = "RETURN_MATERIAL", entityType = "ProductionOrder")
    @PostMapping("/{orderId}/materials/{materialId}/return")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> returnMaterial(
            @PathVariable Long orderId,
            @PathVariable Long materialId,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(new ApiResponse<>("MATERIAL_RETURNED",
                productionOrderService.returnMaterial(orderId, materialId, remarks)));
    }

    // ── STAGE LOGGING ─────────────────────────────────────────────────────────

    @Audited(action = "ADD_STAGE", entityType = "ProductionOrder")
    @PostMapping("/{id}/stages")
    public ResponseEntity<ApiResponse<ProductionOrderResponseDTO>> addStage(
            @PathVariable Long id,
            @Valid @RequestBody StageLogRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("STAGE_LOGGED",
                        productionOrderService.addStageLog(id, dto)));
    }

    // ── ROUTING-DRIVEN PER-BOM-LINE STAGES ────────────────────────────────────

    @GetMapping("/{orderId}/materials/{materialId}/stages")
    public ResponseEntity<ApiResponse<List<StageLogResponseDTO>>> getStagesForMaterial(
            @PathVariable Long orderId, @PathVariable Long materialId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                productionOrderService.getStagesForMaterial(orderId, materialId)));
    }

    @Audited(action = "START_STAGE", entityType = "ProductionOrder")
    @PostMapping("/{orderId}/materials/{materialId}/stages/{stageId}/start")
    public ResponseEntity<ApiResponse<StageLogResponseDTO>> startStage(
            @PathVariable Long orderId, @PathVariable Long materialId, @PathVariable Long stageId,
            @RequestBody StartStageRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("STAGE_STARTED",
                productionOrderService.startStage(orderId, materialId, stageId, dto)));
    }

    @Audited(action = "COMPLETE_STAGE", entityType = "ProductionOrder")
    @PostMapping("/{orderId}/materials/{materialId}/stages/{stageId}/complete-production")
    public ResponseEntity<ApiResponse<StageLogResponseDTO>> completeProductionStage(
            @PathVariable Long orderId, @PathVariable Long materialId, @PathVariable Long stageId,
            @Valid @RequestBody CompleteProductionStageRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("STAGE_COMPLETED",
                productionOrderService.completeProductionStage(orderId, materialId, stageId, dto)));
    }

    @Audited(action = "COMPLETE_INSPECTION_STAGE", entityType = "ProductionOrder")
    @PostMapping("/{orderId}/materials/{materialId}/stages/{stageId}/complete-inspection")
    public ResponseEntity<ApiResponse<StageLogResponseDTO>> completeInspectionStage(
            @PathVariable Long orderId, @PathVariable Long materialId, @PathVariable Long stageId,
            @Valid @RequestBody CompleteStageRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("STAGE_COMPLETED",
                productionOrderService.completeInspectionStage(orderId, materialId, stageId, dto)));
    }

    // ── SHOP-FLOOR NCRs ────────────────────────────────────────────────────────

    @GetMapping("/{orderId}/ncrs")
    public ResponseEntity<ApiResponse<List<ShopFloorNcrResponseDTO>>> getNcrsForOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                productionOrderService.getNcrsForOrder(orderId)));
    }

    @GetMapping("/materials/{materialId}/ncrs")
    public ResponseEntity<ApiResponse<List<ShopFloorNcrResponseDTO>>> getNcrsForMaterial(
            @PathVariable Long materialId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                productionOrderService.getNcrsForMaterial(materialId)));
    }

    // Resolving is a QA-adjacent decision — the class default (PRODUCTION_ENGINEER,
    // MANAGER, ADMIN) doesn't include QA_INSPECTOR, so this needs its own override.
    @Audited(action = "RESOLVE_NCR", entityType = "ShopFloorNcr")
    @PreAuthorize("hasAnyRole('QA_INSPECTOR','MANAGER','ADMIN')")
    @PostMapping("/ncrs/{ncrId}/resolve")
    public ResponseEntity<ApiResponse<ShopFloorNcrResponseDTO>> resolveNcr(
            @PathVariable Long ncrId, @Valid @RequestBody ResolveNcrRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("NCR_RESOLVED",
                productionOrderService.resolveNcr(ncrId, dto.getResolvedBy(), dto.getResolutionRemarks())));
    }
}
