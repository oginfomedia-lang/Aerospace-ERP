package com.aerospace.notification_service.service;

import com.aerospace.notification_service.entity.NotificationLog;
import com.aerospace.notification_service.event.*;

import java.util.List;

public interface NotificationService {

    // ── Triggered by Kafka consumers — Inventory ─────────────────────────────
    void handleStockLow(StockLowEvent event);
    void handleQaStatusChanged(QAStatusChangedEvent event);
    void handleMaterialIssued(MaterialIssuedEvent event);
    void handleStockReceived(StockReceivedEvent event);
    void handlePartCreated(PartCreatedEvent event);
    void handleVendorReturn(VendorReturnEvent event);

    // ── Triggered by Kafka consumers — QA ─────────────────────────────────────
    void handleCalibrationDue(CalibrationDueEvent event);
    void handleFaiCompleted(FaiResultEvent event);
    void handleCapaEvent(CapaEvent event);

    // ── Triggered by Kafka consumers — Work Orders ────────────────────────────
    void handleWorkOrderCreated(WorkOrderCreatedEvent event);
    void handleWorkOrderUpdated(WorkOrderStatusChangedEvent event);
    void handleWorkOrderCompleted(WorkOrderCompletedEvent event);

    // ── REST queries ────────────────────────────────────────────────────────
    List<NotificationLog> getRecent();
    List<NotificationLog> getByPartId(Long partId);
    List<NotificationLog> getByBatchNumber(String batchNumber);
    List<NotificationLog> getByType(String type);

    void handleApprovalEscalated(ApprovalEscalatedEvent event);
}