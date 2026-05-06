package com.aerospace.notification_service.service;

import com.aerospace.notification_service.entity.NotificationLog;
import com.aerospace.notification_service.event.*;

import java.util.List;

public interface NotificationService {

    // ── Triggered by Kafka consumers ────────────────────────────────────────
    void handleStockLow(StockLowEvent event);
    void handleQaStatusChanged(QAStatusChangedEvent event);
    void handleMaterialIssued(MaterialIssuedEvent event);
    void handleStockReceived(StockReceivedEvent event);
    void handlePartCreated(PartCreatedEvent event);

    // ── REST queries ────────────────────────────────────────────────────────
    List<NotificationLog> getRecent();
    List<NotificationLog> getByPartId(Long partId);
    List<NotificationLog> getByBatchNumber(String batchNumber);
    List<NotificationLog> getByType(String type);
}