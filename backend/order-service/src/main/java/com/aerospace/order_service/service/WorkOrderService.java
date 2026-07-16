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
    // re-calling inventory-service. Used by WorkOrderConsumer.onMaterialIssued, since
    // inventory-service already performed the real stock deduction that triggered
    // this event — calling issueMaterial there would deduct a second time.
    WorkOrderResponse recordMaterialIssued(Long workOrderId, Long lineItemId, int quantityIssued, String batchNumber);
    WorkOrderResponse addMaterialLineItem(Long workOrderId, MaterialLineItemRequest request);

    // ── Queries ───────────────────────────────────────────────────────────────
    List<WorkOrderResponse> getByStatus(String status);
    List<WorkOrderResponse> getByPriority(String priority);
    List<WorkOrderResponse> getByAircraft(String aircraftId);
    List<WorkOrderResponse> getOverdue();
    List<WorkOrderResponse> search(String query);
    List<WorkOrderResponse> getRecent();
    List<StatusHistoryResponse> getStatusHistory(Long workOrderId);

    // ── Dashboard ─────────────────────────────────────────────────────────────
    DashboardStatsResponse getDashboardStats();
}