package com.aerospace.notification_service.service;

import com.aerospace.notification_service.entity.NotificationLog;
import com.aerospace.notification_service.event.*;

import java.util.List;
import java.util.Set;

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

    // ── REST queries — each takes the caller's own roles, and only returns
    // notifications relevant to at least one of them (see NotificationRoleMapping).
    List<NotificationLog> getRecent(Set<String> callerRoles);
    List<NotificationLog> getByPartId(Long partId, Set<String> callerRoles);
    List<NotificationLog> getByBatchNumber(String batchNumber, Set<String> callerRoles);
    List<NotificationLog> getByType(String type, Set<String> callerRoles);

    void handleApprovalEscalated(ApprovalEscalatedEvent event);

    void handlePoDeliveryOverdue(PoDeliveryOverdueEvent event);

    void handlePurchaseOrderSentToVendor(PurchaseOrderCreatedEvent event);

    void handleSupplierRegistered(SupplierRegisteredEvent event);
    void handleSupplierConfirmationDecided(SupplierConfirmationDecidedEvent event);

    void handleMaintenanceDue(MaintenanceDueEvent event);

    void handlePasswordResetRequested(PasswordResetRequestedEvent event);

    void handleSalesOrderDispatched(SalesOrderDispatchedEvent event);
}