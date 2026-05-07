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