package com.aerospace.order_service.service;

import com.aerospace.order_service.dto.WorkOrderDTOs.*;

import java.util.List;

public interface WorkOrderService {

    // ── CRUD ──────────────────────────────────────────────────────────────────
    WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request);
    WorkOrderResponse getById(Long id);
    WorkOrderResponse getByWoNumber(String woNumber);
    List<WorkOrderResponse> getAll();
    WorkOrderResponse updateWorkOrder(Long id, UpdateWorkOrderRequest request);
    void deleteWorkOrder(Long id);

    // ── Status Lifecycle ──────────────────────────────────────────────────────
    WorkOrderResponse transitionStatus(Long id, StatusTransitionRequest request);

    // ── Material Management ───────────────────────────────────────────────────
    WorkOrderResponse issueMaterial(Long workOrderId, IssueMaterialRequest request);

    // Bookkeeping-only counterpart to issueMaterial — updates the line's
    // quantityIssued/status/batchNumber and the WO auto-transition check, WITHOUT
    // re-calling inventory-service. Used by:
    //   - issueMaterial() itself, right after inventory confirms the deduction
    //     (eventRef=null — a direct in-process call, nothing to dedup against).
    //   - WorkOrderConsumer.onMaterialIssued, since inventory-service already performed
    //     the real stock deduction that triggered this event — calling issueMaterial
    //     there would deduct a second time. Pass a stable eventRef (e.g. a Kafka
    //     topic-partition-offset key) so a redelivered copy of the same event is
    //     skipped (logged, no-op) instead of double-counting the quantity.
    WorkOrderResponse recordMaterialIssued(Long workOrderId, Long lineItemId, int quantityIssued, String batchNumber, String eventRef);
    WorkOrderResponse addMaterialLineItem(Long workOrderId, MaterialLineItemRequest request);

    // ── Queries ───────────────────────────────────────────────────────────────
    List<WorkOrderResponse> getByStatus(String status);
    List<WorkOrderResponse> getByPriority(String priority);
    List<WorkOrderResponse> getByAircraft(String aircraftId);
    List<WorkOrderResponse> getOverdue();
    List<WorkOrderResponse> search(String query);
    List<WorkOrderResponse> getRecent();
    List<WorkOrderNeedingPartResponse> findWorkOrdersNeedingPart(Long partId);
    List<StatusHistoryResponse> getStatusHistory(Long workOrderId);

    // ── Dashboard ─────────────────────────────────────────────────────────────
    DashboardStatsResponse getDashboardStats();
}