package com.aerospace.notification_service.event.consumer;

import com.aerospace.notification_service.event.*;
import com.aerospace.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    // ── 1. Stock Low ─────────────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.stock-low}",
        groupId          = "notification-group",
        containerFactory = "stockLowListenerFactory"
    )
    public void onStockLow(StockLowEvent event) {
        log.info("[CONSUMER] stock-low-events → partId={}", event.getPartId());
        try {
            notificationService.handleStockLow(event);
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to handle StockLowEvent for partId={}: {}",
                    event.getPartId(), ex.getMessage(), ex);
        }
    }

    // ── 2. QA Status Changed ─────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.qa-status-changed}",
        groupId          = "notification-group",
        containerFactory = "qaStatusListenerFactory"
    )
    public void onQaStatusChanged(QAStatusChangedEvent event) {
        log.info("[CONSUMER] qa-status-changed-events → partId={}, status={}",
                event.getPartId(), event.getNewStatus());
        try {
            notificationService.handleQaStatusChanged(event);
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to handle QAStatusChangedEvent for partId={}: {}",
                    event.getPartId(), ex.getMessage(), ex);
        }
    }

    // ── 3. Material Issued ───────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.material-issued}",
        groupId          = "notification-group",
        containerFactory = "materialIssuedListenerFactory"
    )
    public void onMaterialIssued(MaterialIssuedEvent event) {
        log.info("[CONSUMER] material-issued-events → partId={}, qty={}",
                event.getPartId(), event.getQuantityIssued());
        try {
            notificationService.handleMaterialIssued(event);
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to handle MaterialIssuedEvent for partId={}: {}",
                    event.getPartId(), ex.getMessage(), ex);
        }
    }

    // ── 4. Stock Received ────────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.stock-received}",
        groupId          = "notification-group",
        containerFactory = "stockReceivedListenerFactory"
    )
    public void onStockReceived(StockReceivedEvent event) {
        log.info("[CONSUMER] stock-received-events → partId={}, qty={}",
                event.getPartId(), event.getQuantityReceived());
        try {
            notificationService.handleStockReceived(event);
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to handle StockReceivedEvent for partId={}: {}",
                    event.getPartId(), ex.getMessage(), ex);
        }
    }

    // ── 5. Part Created ──────────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.part-created:part-created-events}",
        groupId          = "notification-group",
        containerFactory = "partCreatedListenerFactory"
    )
    public void onPartCreated(PartCreatedEvent event) {
        log.info("[CONSUMER] part-created-events → partId={}, name={}",
                event.getPartId(), event.getPartName());
        try {
            notificationService.handlePartCreated(event);
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to handle PartCreatedEvent for partId={}: {}",
                    event.getPartId(), ex.getMessage(), ex);
        }
    }
}