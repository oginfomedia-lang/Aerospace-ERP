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

    // ── 10. Calibration Due ───────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.calibration-due}",
        groupId          = "notification-group",
        containerFactory = "calibrationDueListenerFactory"
    )
    public void onCalibrationDue(CalibrationDueEvent event) {
        log.info("[CONSUMER] qa.calibration.due -> assetTag={}, overdue={}",
                event.getAssetTag(), event.isOverdue());
        try { notificationService.handleCalibrationDue(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed CalibrationDueEvent assetTag={}: {}", event.getAssetTag(), ex.getMessage(), ex);
        }
    }

    // ── 11. FAI Completed ─────────────────────────────────────────────────────
    @KafkaListener(
        topics           = NotificationTopics.COMPLIANCE_FAI_COMPLETED,
        groupId          = "notification-group",
        containerFactory = "faiResultListenerFactory"
    )
    public void onFaiCompleted(FaiResultEvent event) {
        log.info("[CONSUMER] compliance.fai.completed -> faiNumber={}, result={}",
                event.getFaiNumber(), event.getOverallResult());
        try { notificationService.handleFaiCompleted(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed FaiResultEvent faiNumber={}: {}", event.getFaiNumber(), ex.getMessage(), ex);
        }
    }

    // ── 12. CAPA Event ────────────────────────────────────────────────────────
    @KafkaListener(
        topics           = NotificationTopics.CAPA_EVENT,
        groupId          = "notification-group",
        containerFactory = "capaEventListenerFactory"
    )
    public void onCapaEvent(CapaEvent event) {
        log.info("[CONSUMER] inventory.capa.event -> capaNumber={} {} -> {}",
                event.getCapaNumber(), event.getFromStatus(), event.getToStatus());
        try { notificationService.handleCapaEvent(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed CapaEvent capaNumber={}: {}", event.getCapaNumber(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
        topics           = "${notification.kafka.topics.approval-escalated}",
        groupId          = "notification-group",
        containerFactory = "approvalEscalatedListenerFactory"
    )
    public void onApprovalEscalated(ApprovalEscalatedEvent event) {
        log.info("[CONSUMER] approval-escalated-events -> {} #{} level {} -> {}",
                event.getModuleCode(), event.getDocumentId(), event.getApprovalLevel(), event.getNewApproverRole());
        try { notificationService.handleApprovalEscalated(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed ApprovalEscalatedEvent {} #{}: {}",
                    event.getModuleCode(), event.getDocumentId(), ex.getMessage(), ex);
        }
    }

    // ── 13. PO Delivery Overdue ───────────────────────────────────────────────
    @KafkaListener(
        topics           = NotificationTopics.PO_DELIVERY_OVERDUE,
        groupId          = "notification-group",
        containerFactory = "poDeliveryOverdueListenerFactory"
    )
    public void onPoDeliveryOverdue(PoDeliveryOverdueEvent event) {
        log.info("[CONSUMER] po-delivery-overdue-events -> po={} supplier={} {}h overdue",
                event.getPoNumber(), event.getSupplierName(), event.getHoursOverdue());
        try { notificationService.handlePoDeliveryOverdue(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed PoDeliveryOverdueEvent po={}: {}", event.getPoNumber(), ex.getMessage(), ex);
        }
    }

    // ── 14. PO Sent to Vendor ─────────────────────────────────────────────────
    @KafkaListener(
        topics           = NotificationTopics.PO_CREATED,
        groupId          = "notification-group",
        containerFactory = "poCreatedListenerFactory"
    )
    public void onPurchaseOrderSentToVendor(PurchaseOrderCreatedEvent event) {
        log.info("[CONSUMER] procurement-po-created -> po={} supplier={}", event.getPoNumber(), event.getSupplierName());
        try { notificationService.handlePurchaseOrderSentToVendor(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed PurchaseOrderCreatedEvent po={}: {}", event.getPoNumber(), ex.getMessage(), ex);
        }
    }

    // ── 15. Supplier Registered ───────────────────────────────────────────────
    @KafkaListener(
        topics           = NotificationTopics.SUPPLIER_REGISTERED,
        groupId          = "notification-group",
        containerFactory = "supplierRegisteredListenerFactory"
    )
    public void onSupplierRegistered(SupplierRegisteredEvent event) {
        log.info("[CONSUMER] supplier-registered-events -> supplier={}", event.getSupplierName());
        try { notificationService.handleSupplierRegistered(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed SupplierRegisteredEvent supplier={}: {}", event.getSupplierName(), ex.getMessage(), ex);
        }
    }

    // ── 16. Supplier Confirmation Decided ─────────────────────────────────────
    @KafkaListener(
        topics           = NotificationTopics.SUPPLIER_CONFIRMATION_DECIDED,
        groupId          = "notification-group",
        containerFactory = "supplierConfirmationDecidedListenerFactory"
    )
    public void onSupplierConfirmationDecided(SupplierConfirmationDecidedEvent event) {
        log.info("[CONSUMER] supplier-confirmation-decided-events -> supplier={} status={}",
                event.getSupplierName(), event.getStatus());
        try { notificationService.handleSupplierConfirmationDecided(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed SupplierConfirmationDecidedEvent supplier={}: {}", event.getSupplierName(), ex.getMessage(), ex);
        }
    }

    // ── 17. Maintenance Due ───────────────────────────────────────────────────
    @KafkaListener(
        topics           = "${notification.kafka.topics.maintenance-due}",
        groupId          = "notification-group",
        containerFactory = "maintenanceDueListenerFactory"
    )
    public void onMaintenanceDue(MaintenanceDueEvent event) {
        log.info("[CONSUMER] production-resource.maintenance.due -> machineCode={}, overdue={}",
                event.getMachineCode(), event.isOverdue());
        try { notificationService.handleMaintenanceDue(event); }
        catch (Exception ex) {
            log.error("[CONSUMER] Failed MaintenanceDueEvent machineCode={}: {}", event.getMachineCode(), ex.getMessage(), ex);
        }
    }
}