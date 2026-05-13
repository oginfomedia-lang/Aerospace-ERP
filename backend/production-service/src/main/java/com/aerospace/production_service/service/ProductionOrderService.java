package com.aerospace.production_service.service;

import com.aerospace.production_service.dto.*;
import com.aerospace.production_service.entity.ProductionOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductionOrderService {

    // ── CRUD ──────────────────────────────────────────────────────────────────
    ProductionOrderResponseDTO createOrder(ProductionOrderRequestDTO dto);
    ProductionOrderResponseDTO getById(Long id);
    ProductionOrderResponseDTO getByPoNumber(String poNumber);
    List<ProductionOrderResponseDTO> getAll();
    Page<ProductionOrderResponseDTO> getAllPaged(Pageable pageable);
    ProductionOrderResponseDTO updateOrder(Long id, ProductionOrderRequestDTO dto);
    void deleteOrder(Long id);

    // ── Lifecycle Transitions ────────────────────────────────────────────────
    ProductionOrderResponseDTO releaseOrder(Long id);
    ProductionOrderResponseDTO startProduction(Long id);
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

    // ── Queries ───────────────────────────────────────────────────────────────
    List<ProductionOrderResponseDTO> getByStatus(ProductionOrderStatus status);
    List<ProductionOrderResponseDTO> searchByProductName(String name);
    List<ProductionOrderResponseDTO> getOverdueOrders();
}
