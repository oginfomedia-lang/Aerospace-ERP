package com.aerospace.production_service.service;

import com.aerospace.production_service.dto.*;
import com.aerospace.production_service.entity.ProductionOrderStatus;
import com.aerospace.production_service.event.WorkOrderStatusChangedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductionOrderService {

    // ── CRUD ──────────────────────────────────────────────────────────────────
    ProductionOrderResponseDTO createOrder(ProductionOrderRequestDTO dto);

    // Auto-BOM: creates a ProductionOrder + ProductionMaterial BOM lines from a
    // WorkOrder's OPEN→IN_PROGRESS transition (see WorkOrderConsumer). Idempotent —
    // a no-op if a ProductionOrder already exists for this workOrderId.
    void createFromWorkOrderIssuance(WorkOrderStatusChangedEvent event);
    ProductionOrderResponseDTO getById(Long id);
    ProductionOrderResponseDTO getByPoNumber(String poNumber);
    List<ProductionOrderResponseDTO> getAll();
    // WO<->Production visibility: the ProductionOrder(s) auto-created from a given
    // WorkOrder (see WorkOrderConsumer) — usually 0 or 1, but not enforced unique.
    List<ProductionOrderResponseDTO> getByWorkOrderId(Long workOrderId);
    Page<ProductionOrderResponseDTO> getAllPaged(Pageable pageable);
    ProductionOrderResponseDTO updateOrder(Long id, ProductionOrderRequestDTO dto);
    void deleteOrder(Long id);

    // ── Lifecycle Transitions ────────────────────────────────────────────────
    ProductionOrderResponseDTO releaseOrder(Long id);
    // skipRoutingForMaterialIds: supervisor override — BOM lines that need no routing
    // (e.g. a bought-complete part), so a missing RoutingTemplate doesn't block start.
    ProductionOrderResponseDTO startProduction(Long id, List<Long> skipRoutingForMaterialIds);
    ProductionOrderResponseDTO moveToQualityCheck(Long id, Integer producedQty, Integer rejectedQty);
    ProductionOrderResponseDTO completeOrder(Long id, Boolean qualityPassed,
                                             String qualityNotes, String closureNotes);
    ProductionOrderResponseDTO putOnHold(Long id, String reason);
    ProductionOrderResponseDTO cancelOrder(Long id, String reason);

    // ── Material Management ──────────────────────────────────────────────────
    ProductionOrderResponseDTO consumeMaterial(Long orderId, Long materialId,
                                               int consumed, int wasted, String remarks);
    ProductionOrderResponseDTO returnMaterial(Long orderId, Long materialId, String remarks);

    // ── Stage Logging ────────────────────────────────────────────────────────
    ProductionOrderResponseDTO addStageLog(Long orderId, StageLogRequestDTO dto);

    // ── Routing-driven per-BOM-line stages ──────────────────────────────────
    StageLogResponseDTO startStage(Long orderId, Long materialId, Long stageId, StartStageRequestDTO dto);
    // A PRODUCTION-type stage — reports quantity produced only, no accept/reject
    // decision, no role restriction beyond normal authentication.
    StageLogResponseDTO completeProductionStage(Long orderId, Long materialId, Long stageId, CompleteProductionStageRequestDTO dto);
    // A QA_INSPECTION-type stage — the good/rework/scrap decision. Role-gated to
    // QA_INSPECTOR/MANAGER/ADMIN.
    StageLogResponseDTO completeInspectionStage(Long orderId, Long materialId, Long stageId, CompleteStageRequestDTO dto);
    List<StageLogResponseDTO> getStagesForMaterial(Long orderId, Long materialId);

    // ── Final assembly ───────────────────────────────────────────────────────
    ProductionOrderResponseDTO startAssembly(Long orderId);

    // ── Shop-floor NCRs ───────────────────────────────────────────────────────
    List<ShopFloorNcrResponseDTO> getNcrsForOrder(Long orderId);
    List<ShopFloorNcrResponseDTO> getNcrsForMaterial(Long materialId);

    // ── Queries ───────────────────────────────────────────────────────────────
    List<ProductionOrderResponseDTO> getByStatus(ProductionOrderStatus status);
    List<ProductionOrderResponseDTO> searchByProductName(String name);
    List<ProductionOrderResponseDTO> getOverdueOrders();
}
