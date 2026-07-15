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

    // ── 1. Stock Low ──────────────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.stock-low}",
        groupId          = "notification-group",
        containerFactory = "stockLowListenerFactory"
    )
    public void onStockLow(StockLowEvent event) {
        log.info("[CONSUMER] stock-low-events → partId={}", event.getPartId());
        try { notificationService.handleStockLow(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed StockLowEvent partId={}: {}", event.getPartId(), ex.getMessage(), ex);
        }
    }

    // ── 2. QA Status Changed ──────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.qa-status-changed}",
        groupId          = "notification-group",
        containerFactory = "qaStatusListenerFactory"
    )
    public void onQaStatusChanged(QAStatusChangedEvent event) {
        log.info("[CONSUMER] qa-status-changed-events → partId={}, status={}",
                event.getPartId(), event.getNewStatus());
        try { notificationService.handleQaStatusChanged(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed QAStatusChangedEvent partId={}: {}", event.getPartId(), ex.getMessage(), ex);
        }
    }

    // ── 3. Material Issued ────────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.material-issued}",
        groupId          = "notification-group",
        containerFactory = "materialIssuedListenerFactory"
    )
    public void onMaterialIssued(MaterialIssuedEvent event) {
        log.info("[CONSUMER] material-issued-events → partId={}, qty={}",
                event.getPartId(), event.getQuantityIssued());
        try { notificationService.handleMaterialIssued(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed MaterialIssuedEvent partId={}: {}", event.getPartId(), ex.getMessage(), ex);
        }
    }

    // ── 4. Stock Received ─────────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.stock-received}",
        groupId          = "notification-group",
        containerFactory = "stockReceivedListenerFactory"
    )
    public void onStockReceived(StockReceivedEvent event) {
        log.info("[CONSUMER] stock-received-events → partId={}, qty={}",
                event.getPartId(), event.getQuantityReceived());
        try { notificationService.handleStockReceived(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed StockReceivedEvent partId={}: {}", event.getPartId(), ex.getMessage(), ex);
        }
    }

    // ── 5. Part Created ───────────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.part-created}",
        groupId          = "notification-group",
        containerFactory = "partCreatedListenerFactory"
    )
    public void onPartCreated(PartCreatedEvent event) {
        log.info("[CONSUMER] part-created-events → partId={}, name={}",
                event.getPartId(), event.getPartName());
        try { notificationService.handlePartCreated(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed PartCreatedEvent partId={}: {}", event.getPartId(), ex.getMessage(), ex);
        }
    }

    // ── 6. Work Order Created ─────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.work-order-created:work-order-created-events}",
        groupId          = "notification-group",
        containerFactory = "workOrderCreatedListenerFactory"
    )
    public void onWorkOrderCreated(WorkOrderCreatedEvent event) {
        log.info("[CONSUMER] work-order-created-events → woNumber={}, aircraft={}",
                event.getWoNumber(), event.getAircraftId());
        try { notificationService.handleWorkOrderCreated(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed WorkOrderCreatedEvent woNumber={}: {}", event.getWoNumber(), ex.getMessage(), ex);
        }
    }

    // ── 7. Work Order Status Updated ──────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.work-order-updated:work-order-updated-events}",
        groupId          = "notification-group",
        containerFactory = "workOrderUpdatedListenerFactory"
    )
    public void onWorkOrderUpdated(WorkOrderStatusChangedEvent event) {
        log.info("[CONSUMER] work-order-updated-events → woNumber={} {} → {}",
                event.getWoNumber(), event.getFromStatus(), event.getToStatus());
        try { notificationService.handleWorkOrderUpdated(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed WorkOrderStatusChangedEvent woNumber={}: {}", event.getWoNumber(), ex.getMessage(), ex);
        }
    }

    // ── 8. Work Order Completed ───────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.work-order-completed:work-order-completed-events}",
        groupId          = "notification-group",
        containerFactory = "workOrderCompletedListenerFactory"
    )
    public void onWorkOrderCompleted(WorkOrderCompletedEvent event) {
        log.info("[CONSUMER] work-order-completed-events → woNumber={}", event.getWoNumber());
        try { notificationService.handleWorkOrderCompleted(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed WorkOrderCompletedEvent woNumber={}: {}", event.getWoNumber(), ex.getMessage(), ex);
        }
    }

    // --9. Vendor Return ─────────────────────────────────────────────────────────────
    @KafkaListener(
        topics          = "${notification.kafka.topics.vendor-return:vendor-return-events}",
        groupId          = "notification-group",
        containerFactory = "vendorReturnListenerFactory"
    )
    public void onVendorReturn(VendorReturnEvent event) {
        log.info("[CONSUMER] vendor-return-events -> ncrNumber={}, skuCode={}",
                event.getNcrNumber(), event.getSkuCode());
        try { notificationService.handleVendorReturn(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed VendorReturnEvent ncrNumber={}: {}", event.getNcrNumber(), ex.getMessage(), ex);
        }
    }
}