package com.aerospace.notification_service.service.impl;

import com.aerospace.notification_service.entity.NotificationLog;
import com.aerospace.notification_service.entity.NotificationStatus;
import com.aerospace.notification_service.entity.NotificationType;
import com.aerospace.notification_service.event.*;
import com.aerospace.notification_service.repository.NotificationRepository;
import com.aerospace.notification_service.service.EmailService;
import com.aerospace.notification_service.service.NotificationService;
import com.aerospace.notification_service.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final WebSocketService webSocketService;
    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    // ── Inventory Kafka Event Handlers ────────────────────────────────────────

    @Override
    public void handleStockLow(StockLowEvent event) {
        log.info("[NOTIFY] LOW_STOCK → partId={}, qty={}, reorderLevel={}",
                event.getPartId(), event.getCurrentQuantity(), event.getReorderLevel());

        String message = String.format(
                "⚠️ LOW STOCK ALERT: Part '%s' (Batch: %s) has only %d units remaining. " +
                        "Reorder level is %d. Location: %s",
                event.getPartName(), event.getBatchNumber(),
                event.getCurrentQuantity(), event.getReorderLevel(), event.getLocation());

        dispatch(NotificationLog.builder()
                .type(NotificationType.LOW_STOCK)
                .partId(event.getPartId())
                .partName(event.getPartName())
                .batchNumber(event.getBatchNumber())
                .message(message)
                .location(event.getLocation())
                .severity("WARNING")
                .build(), message);
    }

    @Override
    public void handleQaStatusChanged(QAStatusChangedEvent event) {
        String status = event.getNewStatus() != null ? event.getNewStatus() : event.getDecision();
        log.info("[NOTIFY] QA_STATUS_CHANGED → partId={}, status={}, expired={}",
                event.getPartId(), status, event.isBatchExpired());

        NotificationType type;
        String severity;
        String message;

        if (event.isBatchExpired()) {
            type = NotificationType.BATCH_EXPIRED;
            severity = "ERROR";
            message = String.format(
                    "❌ BATCH EXPIRED: Part '%s' (Batch: %s) was REJECTED — batch expired on %s. " +
                            "Immediate disposal required. Inspector: %s | Location: %s",
                    event.getPartName(), event.getBatchNumber(), event.getExpiryDate(),
                    event.getInspectorName(), event.getLocation());
        } else {
            type = switch (status.toUpperCase()) {
                case "APPROVED" -> NotificationType.QA_APPROVED;
                case "REJECTED" -> NotificationType.QA_REJECTED;
                default         -> NotificationType.QA_PENDING;
            };
            severity = switch (type) {
                case QA_APPROVED -> "SUCCESS";
                case QA_REJECTED -> "ERROR";
                default          -> "INFO";
            };
            message = switch (type) {
                case QA_APPROVED -> String.format(
                        "✅ QA APPROVED: Part '%s' (Batch: %s) has passed inspection. " +
                                "Inspector: %s | Location: %s | Remarks: %s",
                        event.getPartName(), event.getBatchNumber(),
                        event.getInspectorName(), event.getLocation(), event.getRemarks());
                case QA_REJECTED -> String.format(
                        "❌ QA REJECTED: Part '%s' (Batch: %s) failed inspection. " +
                                "Inspector: %s | Location: %s | Reason: %s",
                        event.getPartName(), event.getBatchNumber(),
                        event.getInspectorName(), event.getLocation(), event.getRemarks());
                default -> String.format(
                        "🔍 QA PENDING: Part '%s' (Batch: %s) is awaiting QA inspection at %s.",
                        event.getPartName(), event.getBatchNumber(), event.getLocation());
            };
        }

        dispatch(NotificationLog.builder()
                .type(type)
                .partId(event.getPartId())
                .partName(event.getPartName())
                .batchNumber(event.getBatchNumber())
                .message(message)
                .inspectorName(event.getInspectorName())
                .location(event.getLocation())
                .severity(severity)
                .build(), message);
    }

    @Override
    public void handleMaterialIssued(MaterialIssuedEvent event) {
        log.info("[NOTIFY] MATERIAL_ISSUED → partId={}, qty={}, issuedTo={}",
                event.getPartId(), event.getQuantityIssued(), event.getIssuedTo());

        String message = String.format(
                "📦 MATERIAL ISSUED: %d units of '%s' (Batch: %s) issued to %s by %s. " +
                        "Remaining stock: %d. Location: %s",
                event.getQuantityIssued(), event.getPartName(), event.getBatchNumber(),
                event.getIssuedTo(), event.getIssuedBy(),
                event.getRemainingQuantity(), event.getLocation());

        dispatch(NotificationLog.builder()
                .type(NotificationType.MATERIAL_ISSUED)
                .partId(event.getPartId())
                .partName(event.getPartName())
                .batchNumber(event.getBatchNumber())
                .message(message)
                .location(event.getLocation())
                .severity("INFO")
                .build(), message);
    }

    @Override
    public void handleStockReceived(StockReceivedEvent event) {
        log.info("[NOTIFY] STOCK_RECEIVED → partId={}, qty={}, supplier={}",
                event.getPartId(), event.getQuantityReceived(), event.getSupplier());

        String message = String.format(
                "📥 STOCK RECEIVED: %d units of '%s' (Batch: %s) received from %s. " +
                        "Batch is PENDING QA inspection. Location: %s | Received by: %s",
                event.getQuantityReceived(), event.getPartName(), event.getBatchNumber(),
                event.getSupplier(), event.getLocation(), event.getReceivedBy());

        dispatch(NotificationLog.builder()
                .type(NotificationType.STOCK_RECEIVED)
                .partId(event.getPartId())
                .partName(event.getPartName())
                .batchNumber(event.getBatchNumber())
                .message(message)
                .location(event.getLocation())
                .severity("INFO")
                .build(), message);
    }

    @Override
    public void handlePartCreated(PartCreatedEvent event) {
        log.info("[NOTIFY] PART_CREATED → partId={}, name={}", event.getPartId(), event.getPartName());

        String message = String.format(
                "🆕 NEW PART REGISTERED: '%s' (Batch: %s) added to inventory by %s. " +
                        "Initial qty: %d | Reorder level: %d | Location: %s",
                event.getPartName(), event.getBatchNumber(), event.getCreatedBy(),
                event.getInitialQuantity(), event.getReorderLevel(), event.getLocation());

        dispatch(NotificationLog.builder()
                .type(NotificationType.PART_CREATED)
                .partId(event.getPartId())
                .partName(event.getPartName())
                .batchNumber(event.getBatchNumber())
                .message(message)
                .location(event.getLocation())
                .severity("INFO")
                .build(), message);
    }

        @Override
    public void handleVendorReturn(VendorReturnEvent event) {
        log.info("[NOTIFY] VENDOR_RETURN → ncrNumber={}, skuCode={}, qty={}",
                event.getNcrNumber(), event.getSkuCode(), event.getQuantity());

        String message = String.format(
                "↩️ VENDOR RETURN: %d units of '%s' (Batch: %s) returned to vendor under NCR %s — a debit note has been raised (PO: %s).",
                event.getQuantity(), event.getPartName(), event.getBatchNumber(),
                event.getNcrNumber(), event.getPoNumber());

        dispatch(NotificationLog.builder()
                .type(NotificationType.VENDOR_RETURN)
                .partId(event.getPartId())
                .partName(event.getPartName())
                .batchNumber(event.getBatchNumber())
                .message(message)
                .severity("INFO")
                .build(), message);
    }


    // ── Work Order Kafka Event Handlers ───────────────────────────────────────

    @Override
    public void handleWorkOrderCreated(WorkOrderCreatedEvent event) {
        log.info("[NOTIFY] WORK_ORDER_CREATED → woNumber={}, aircraft={}, priority={}",
                event.getWoNumber(), event.getAircraftId(), event.getPriority());

        String severity = "CRITICAL".equals(event.getPriority()) ? "ERROR" : "INFO";
        String message = String.format(
                "🔧 WORK ORDER CREATED: %s raised for aircraft %s. Task: %s. " +
                        "Priority: %s | Location: %s | Created by: %s",
                event.getWoNumber(), event.getAircraftId(),
                event.getTaskDescription(), event.getPriority(),
                event.getLocation() != null ? event.getLocation() : "—",
                event.getCreatedBy() != null ? event.getCreatedBy() : "System");

        dispatch(NotificationLog.builder()
                .type(NotificationType.WORK_ORDER_CREATED)
                .partId(event.getWorkOrderId())       // reusing partId field for WO ID
                .partName(event.getWoNumber())        // reusing partName for WO number
                .batchNumber(event.getAircraftId())   // reusing batchNumber for aircraft ID
                .message(message)
                .location(event.getLocation())
                .severity(severity)
                .build(), message);
    }

    @Override
    public void handleWorkOrderUpdated(WorkOrderStatusChangedEvent event) {
        log.info("[NOTIFY] WORK_ORDER_UPDATED → woNumber={}, {} → {}",
                event.getWoNumber(), event.getFromStatus(), event.getToStatus());

        String severity = switch (event.getToStatus() != null ? event.getToStatus() : "") {
            case "COMPLETED" -> "SUCCESS";
            case "ON_HOLD"   -> "WARNING";
            case "CANCELLED" -> "ERROR";
            default          -> "INFO";
        };

        String icon = switch (event.getToStatus() != null ? event.getToStatus() : "") {
            case "IN_PROGRESS" -> "▶️";
            case "ON_HOLD"     -> "⏸️";
            case "COMPLETED"   -> "✅";
            case "CANCELLED"   -> "❌";
            case "OPEN"        -> "📋";
            default            -> "🔄";
        };

        String message = String.format(
                "%s WORK ORDER %s: %s moved from %s → %s. Aircraft: %s | Changed by: %s | Remarks: %s",
                icon, event.getToStatus(), event.getWoNumber(),
                event.getFromStatus() != null ? event.getFromStatus() : "—",
                event.getToStatus(), event.getAircraftId(),
                event.getChangedBy() != null ? event.getChangedBy() : "System",
                event.getRemarks()   != null ? event.getRemarks()   : "—");

        dispatch(NotificationLog.builder()
                .type(NotificationType.WORK_ORDER_UPDATED)
                .partId(event.getWorkOrderId())
                .partName(event.getWoNumber())
                .batchNumber(event.getAircraftId())
                .message(message)
                .location(null)
                .severity(severity)
                .build(), message);
    }

    @Override
    public void handleWorkOrderCompleted(WorkOrderCompletedEvent event) {
        log.info("[NOTIFY] WORK_ORDER_COMPLETED → woNumber={}, aircraft={}",
                event.getWoNumber(), event.getAircraftId());

        String message = String.format(
                "✅ WORK ORDER COMPLETED: %s for aircraft %s has been closed. " +
                        "Task: %s | Completed by: %s | Notes: %s",
                event.getWoNumber(), event.getAircraftId(),
                event.getTaskDescription(),
                event.getCompletedBy()  != null ? event.getCompletedBy()  : "System",
                event.getClosureNotes() != null ? event.getClosureNotes() : "—");

        dispatch(NotificationLog.builder()
                .type(NotificationType.WORK_ORDER_COMPLETED)
                .partId(event.getWorkOrderId())
                .partName(event.getWoNumber())
                .batchNumber(event.getAircraftId())
                .message(message)
                .location(null)
                .severity("SUCCESS")
                .build(), message);
    }

    // ── REST Queries ──────────────────────────────────────────────────────────

    @Override
    public List<NotificationLog> getRecent() {
        return notificationRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @Override
    public List<NotificationLog> getByPartId(Long partId) {
        return notificationRepository.findByPartIdOrderByCreatedAtDesc(partId);
    }

    @Override
    public List<NotificationLog> getByBatchNumber(String batchNumber) {
        return notificationRepository.findByBatchNumberOrderByCreatedAtDesc(batchNumber);
    }

    @Override
    public List<NotificationLog> getByType(String type) {
        return notificationRepository.findByTypeOrderByCreatedAtDesc(
                NotificationType.valueOf(type.toUpperCase()));
    }

    // ── Orchestration ─────────────────────────────────────────────────────────

    private void dispatch(NotificationLog log_, String message) {
        boolean wsSent    = false;
        boolean emailSent = false;
        String  errorMsg  = null;

        try {
            webSocketService.broadcast(log_);
            wsSent = true;
        } catch (Exception ex) {
            log.error("[NOTIFY] WebSocket dispatch failed for partId={}: {}",
                    log_.getPartId(), ex.getMessage());
            errorMsg = "WS: " + ex.getMessage();
        }

        try {
            emailService.sendAsync(log_);
            emailSent = true;
        } catch (Exception ex) {
            log.error("[NOTIFY] Email dispatch failed for partId={}: {}",
                    log_.getPartId(), ex.getMessage());
            errorMsg = (errorMsg != null ? errorMsg + " | " : "") + "EMAIL: " + ex.getMessage();
        }

        NotificationStatus status = (wsSent || emailSent)
                ? NotificationStatus.SENT
                : NotificationStatus.FAILED;

        log_.setWebsocketSent(wsSent);
        log_.setEmailSent(emailSent);
        log_.setStatus(status);
        log_.setSentAt(LocalDateTime.now());
        log_.setErrorMessage(errorMsg);

        notificationRepository.save(log_);
        log.info("[NOTIFY] ✅ Persisted notification | type={} partId={} status={}",
                log_.getType(), log_.getPartId(), status);
    }
}