package com.aerospace.notification_service.service.impl;

import com.aerospace.notification_service.entity.NotificationLog;
import com.aerospace.notification_service.entity.NotificationStatus;
import com.aerospace.notification_service.entity.NotificationType;
import com.aerospace.notification_service.event.*;
import com.aerospace.notification_service.repository.NotificationRepository;
import com.aerospace.notification_service.service.EmailService;
import com.aerospace.notification_service.service.NotificationService;
import com.aerospace.notification_service.service.WebSocketService;
import com.aerospace.notification_service.util.NotificationRoleMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
                .actorUsername(event.getInspectorName())
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
                .actorUsername(event.getIssuedBy())
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
                .actorUsername(event.getReceivedBy())
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
                .actorUsername(event.getCreatedBy())
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
                .actorUsername(event.getDispositionedBy())
                .severity("INFO")
                .build(), message);
    }


    // ── QA Kafka Event Handlers ───────────────────────────────────────────────

    @Override
    public void handleCalibrationDue(CalibrationDueEvent event) {
        log.info("[NOTIFY] CALIBRATION_DUE → assetTag={}, overdue={}, daysUntilDue={}",
                event.getAssetTag(), event.isOverdue(), event.getDaysUntilDue());

        String severity = event.isOverdue() ? "ERROR" : "WARNING";
        String message = event.isOverdue()
                ? String.format(
                        "⏰ CALIBRATION OVERDUE: %s ('%s', %s) was due for calibration on %s. Location: %s",
                        event.getAssetTag(), event.getEquipmentName(), event.getEquipmentType(),
                        event.getNextCalibrationDueAt(), event.getLocation())
                : String.format(
                        "⏰ CALIBRATION DUE SOON: %s ('%s', %s) is due for calibration in %d day(s) (%s). Location: %s",
                        event.getAssetTag(), event.getEquipmentName(), event.getEquipmentType(),
                        event.getDaysUntilDue(), event.getNextCalibrationDueAt(), event.getLocation());

        dispatch(NotificationLog.builder()
                .type(NotificationType.CALIBRATION_DUE)
                .partName(event.getEquipmentName())    // reusing partName for equipment name
                .batchNumber(event.getAssetTag())      // reusing batchNumber for asset tag
                .message(message)
                .location(event.getLocation())
                .severity(severity)
                .build(), message);
    }

    @Override
    public void handleFaiCompleted(FaiResultEvent event) {
        log.info("[NOTIFY] FAI_COMPLETED → faiNumber={}, result={}", event.getFaiNumber(), event.getOverallResult());

        boolean isConforming = "CONFORMING".equalsIgnoreCase(event.getOverallResult());
        String severity = isConforming ? "INFO" : "ERROR";
        String message = String.format(
                "📋 FIRST ARTICLE INSPECTION %s: FAI %s for Part '%s' (%s) resulted in %s by Inspector %s. Remarks: %s",
                isConforming ? "PASSED" : "FAILED",
                event.getFaiNumber(), event.getPartName(), event.getSkuCode() != null ? event.getSkuCode() : "N/A",
                event.getOverallResult(), event.getInspectorName() != null ? event.getInspectorName() : "Inspector",
                event.getRemarks() != null ? event.getRemarks() : "—");

        dispatch(NotificationLog.builder()
                .type(NotificationType.FAI_COMPLETED)
                .partId(event.getPartId())
                .partName(event.getPartName())
                .batchNumber(event.getBatchNumber())
                .message(message)
                .actorUsername(event.getInspectorName())
                .severity(severity)
                .build(), message);
    }

    @Override
    public void handleCapaEvent(CapaEvent event) {
        log.info("[NOTIFY] CAPA_EVENT → capaNumber={}, status={} -> {}", event.getCapaNumber(), event.getFromStatus(), event.getToStatus());

        String severity;
        String message;
        if (event.isOverdue()) {
            severity = "WARNING";
            message = String.format("⏰ CAPA OVERDUE: %s is past its target close date. %s",
                    event.getCapaNumber(), event.getRemarks() != null ? event.getRemarks() : "");
        } else {
            severity = "CLOSED".equalsIgnoreCase(event.getToStatus()) ? "INFO"
                    : "REJECTED".equalsIgnoreCase(event.getToStatus()) ? "WARNING"
                    : "INFO";
            message = String.format(
                    "🛡️ CAPA UPDATE: %s (%s) transitioned from %s to %s by %s. Remarks: %s",
                    event.getCapaNumber(), event.getPartName() != null ? event.getPartName() : "NCR " + event.getNcrNumber(),
                    event.getFromStatus() != null ? event.getFromStatus() : "OPEN", event.getToStatus(),
                    event.getActor() != null ? event.getActor() : "System",
                    event.getRemarks() != null ? event.getRemarks() : "—");
        }

        dispatch(NotificationLog.builder()
                .type(event.isOverdue() ? NotificationType.CAPA_OVERDUE : NotificationType.CAPA_STATUS_CHANGED)
                .batchNumber(event.getBatchNumber())
                .message(message)
                .actorUsername(event.getActor())
                .severity(severity)
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
                .actorUsername(event.getCreatedBy())
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
                .actorUsername(event.getChangedBy())
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
                .actorUsername(event.getCompletedBy())
                .location(null)
                .severity("SUCCESS")
                .build(), message);
    }

    // ── REST Queries — each filtered down to only what the caller's own
    // role(s) are allowed to see, using the exact same list the live WebSocket
    // popups already use (NotificationRoleMapping), so the two never disagree. ──

    @Override
    public List<NotificationLog> getRecent(Set<String> callerRoles) {
        return filterForRoles(notificationRepository.findTop20ByOrderByCreatedAtDesc(), callerRoles);
    }

    @Override
    public List<NotificationLog> getByPartId(Long partId, Set<String> callerRoles) {
        return filterForRoles(notificationRepository.findByPartIdOrderByCreatedAtDesc(partId), callerRoles);
    }

    @Override
    public List<NotificationLog> getByBatchNumber(String batchNumber, Set<String> callerRoles) {
        return filterForRoles(notificationRepository.findByBatchNumberOrderByCreatedAtDesc(batchNumber), callerRoles);
    }

    @Override
    public List<NotificationLog> getByType(String type, Set<String> callerRoles) {
        return filterForRoles(notificationRepository.findByTypeOrderByCreatedAtDesc(
                NotificationType.valueOf(type.toUpperCase())), callerRoles);
    }

    // Keeps only the notifications whose type is actually relevant to at least
    // one of the caller's own roles.
    private List<NotificationLog> filterForRoles(List<NotificationLog> logs, Set<String> callerRoles) {
        return logs.stream()
                .filter(n -> !Collections.disjoint(NotificationRoleMapping.rolesFor(n.getType()), callerRoles))
                .toList();
    }

    // ── Orchestration ─────────────────────────────────────────────────────────

    private void dispatch(NotificationLog log_, String message) {
        boolean wsSent    = false;
        boolean emailSent = false;
        String  errorMsg  = null;

        try {
            webSocketService.broadcast(log_, log_.getActorUsername());
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

    @Override
    public void handleApprovalEscalated(ApprovalEscalatedEvent event) {
        log.info("[NOTIFY] APPROVAL_ESCALATED → {} #{} level {} -> {}",
                event.getModuleCode(), event.getDocumentId(), event.getApprovalLevel(), event.getNewApproverRole());

        String message = String.format(
                "⚠️ APPROVAL ESCALATED: %s #%d (level %d) was reassigned to %s — pending since %s with no action.",
                event.getModuleCode(), event.getDocumentId(), event.getApprovalLevel(),
                event.getNewApproverRole(), event.getPendingSince());

        dispatch(NotificationLog.builder()
                .type(NotificationType.APPROVAL_ESCALATED)
                .message(message)
                .severity("WARNING")
                .build(), message);
    }

    @Override
    public void handlePoDeliveryOverdue(PoDeliveryOverdueEvent event) {
        log.info("[NOTIFY] PO_DELIVERY_OVERDUE → po={}, supplier={}, {}h overdue",
                event.getPoNumber(), event.getSupplierName(), event.getHoursOverdue());

        String message = String.format(
                "🚚 PO DELIVERY OVERDUE: %s from %s was due %s and hasn't arrived (%dh overdue).",
                event.getPoNumber(), event.getSupplierName() != null ? event.getSupplierName() : "supplier",
                event.getDeliveryDate(), event.getHoursOverdue());

        dispatch(NotificationLog.builder()
                .type(NotificationType.PO_DELIVERY_OVERDUE)
                .partName(event.getPoNumber())   // reusing partName for the PO number, same convention WO events use
                .message(message)
                .severity("WARNING")
                .build(), message);
    }

    @Override
    public void handlePurchaseOrderSentToVendor(PurchaseOrderCreatedEvent event) {
        log.info("[NOTIFY] PO_SENT_TO_VENDOR → po={}, supplier={}, amount={} {}",
                event.getPoNumber(), event.getSupplierName(), event.getCurrency(), event.getTotalAmount());

        String message = String.format(
                "📤 PO SENT TO VENDOR: %s issued to %s — %s %.2f, expected delivery %s.",
                event.getPoNumber(), event.getSupplierName() != null ? event.getSupplierName() : "supplier",
                event.getCurrency(), event.getTotalAmount() != null ? event.getTotalAmount() : 0.0,
                event.getExpectedDeliveryDate());

        // Internal visibility — same in-app/ops-email path every other notification uses.
        dispatch(NotificationLog.builder()
                .type(NotificationType.PO_SENT_TO_VENDOR)
                .partName(event.getPoNumber())
                .message(message)
                .severity("INFO")
                .build(), message);

        // dispatch() above only ever emails the internal ops list (notification.email.recipients) —
        // this is the part that actually reaches the vendor, sent directly to their own address.
        if (event.getSupplierEmail() != null && !event.getSupplierEmail().isBlank()) {
            String subject = "Purchase Order " + event.getPoNumber() + " from Aerospace ERP";
            String html = String.format("""
                    <p>Dear %s,</p>
                    <p>Purchase Order <b>%s</b> has been issued to you.</p>
                    <ul>
                      <li>Amount: %s %.2f</li>
                      <li>Expected delivery: %s</li>
                    </ul>
                    <p>Please log in to the Supplier Portal to view full details and confirm.</p>
                    """,
                    event.getSupplierName() != null ? event.getSupplierName() : "Supplier",
                    event.getPoNumber(),
                    event.getCurrency(), event.getTotalAmount() != null ? event.getTotalAmount() : 0.0,
                    event.getExpectedDeliveryDate());
            emailService.sendCustomEmail(event.getSupplierEmail(), subject, html);
        } else {
            log.warn("[NOTIFY] No email on file for supplier {} — PO {} not emailed, only logged internally.",
                    event.getSupplierName(), event.getPoNumber());
        }
    }

    @Override
    public void handleSupplierRegistered(SupplierRegisteredEvent event) {
        log.info("[NOTIFY] SUPPLIER_PENDING_CONFIRMATION → supplier={}", event.getSupplierName());

        String message = String.format(
                "🕵️ NEW SUPPLIER PENDING CONFIRMATION: '%s' (Code: %s) was added by %s and needs QA/authority "
                        + "sign-off before it can be invited to the Supplier Portal.",
                event.getSupplierName(), event.getVendorCode(),
                event.getRegisteredBy() != null ? event.getRegisteredBy() : "System");

        dispatch(NotificationLog.builder()
                .type(NotificationType.SUPPLIER_PENDING_CONFIRMATION)
                .partName(event.getSupplierName())
                .message(message)
                .actorUsername(event.getRegisteredBy())
                .severity("INFO")
                .build(), message);
    }

    @Override
    public void handleSupplierConfirmationDecided(SupplierConfirmationDecidedEvent event) {
        boolean confirmed = "CONFIRMED".equalsIgnoreCase(event.getStatus());
        log.info("[NOTIFY] SUPPLIER_{} → supplier={}, by={}", event.getStatus(), event.getSupplierName(), event.getDecidedBy());

        String message = String.format(
                "%s SUPPLIER %s: '%s' was %s by %s. Remarks: %s",
                confirmed ? "✅" : "❌", event.getStatus(), event.getSupplierName(),
                confirmed ? "confirmed" : "rejected", event.getDecidedBy(),
                event.getRemarks() != null ? event.getRemarks() : "—");

        dispatch(NotificationLog.builder()
                .type(confirmed ? NotificationType.SUPPLIER_CONFIRMED : NotificationType.SUPPLIER_REJECTED)
                .partName(event.getSupplierName())
                .message(message)
                .actorUsername(event.getDecidedBy())
                .severity(confirmed ? "SUCCESS" : "WARNING")
                .build(), message);
    }

        @Override
        public void handleMaintenanceDue(MaintenanceDueEvent event) {
        log.info("[NOTIFY] MAINTENANCE_DUE → machineCode={}, overdue={}, daysUntilDue={}",
                event.getMachineCode(), event.isOverdue(), event.getDaysUntilDue());

        String severity = event.isOverdue() ? "ERROR" : "WARNING";
        String message = event.isOverdue()
                ? String.format(
                        "🔧 MAINTENANCE OVERDUE: %s ('%s', %s) was due for maintenance on %s. Location: %s",
                        event.getMachineCode(), event.getMachineName(), event.getMachineType(),
                        event.getNextMaintenanceDueAt(), event.getLocation())
                : String.format(
                        "🔧 MAINTENANCE DUE SOON: %s ('%s', %s) is due for maintenance in %d day(s) (%s). Location: %s",
                        event.getMachineCode(), event.getMachineName(), event.getMachineType(),
                        event.getDaysUntilDue(), event.getNextMaintenanceDueAt(), event.getLocation());

        dispatch(NotificationLog.builder()
                .type(NotificationType.MAINTENANCE_DUE)
                .partName(event.getMachineName())
                .batchNumber(event.getMachineCode())
                .message(message)
                .location(event.getLocation())
                .severity(severity)
                .build(), message);
        }

        @Override
        public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("[NOTIFY] PASSWORD_RESET_REQUESTED → email={}", event.getEmail());

        String subject = "Reset your Aerospace ERP password";
        String html = String.format("""
                <p>Hi %s,</p>
                <p>We received a request to reset your Aerospace ERP password.</p>
                <p><a href="%s" style="display:inline-block;padding:10px 20px;background:#3b82f6;
                    color:#fff;text-decoration:none;border-radius:8px;">Reset Password</a></p>
                <p>Or copy this link into your browser:<br>%s</p>
                <p>This link expires in %d minutes. If you didn't request this, you can safely ignore this email.</p>
                """,
                event.getUsername() != null ? event.getUsername() : "there",
                event.getResetLink(), event.getResetLink(), event.getExpiryMinutes());

        // Sent directly to the requesting user, not the internal ops recipients list —
        // same pattern as handlePurchaseOrderSentToVendor's direct-to-supplier email.
        emailService.sendCustomEmail(event.getEmail(), subject, html);

        // No dispatch()/NotificationLog entry — this is a security-sensitive per-user
        // email, not an internal ops alert; logging it would leak who's resetting
        // passwords to anyone with UI access to the shared notification feed.
        }

         @Override
        public void handleSalesOrderDispatched(SalesOrderDispatchedEvent event) {
                log.info("[NOTIFY] SALES_ORDER_DISPATCHED → orderNumber={}, tracking={}",
                        event.getOrderNumber(), event.getTrackingNumber());

                String message = String.format(
                        "📦 ORDER DISPATCHED: %s has shipped. Dispatch: %s | Carrier tracking: %s",
                        event.getOrderNumber(), event.getDispatchNumber(),
                        event.getTrackingNumber() != null ? event.getTrackingNumber() : "—");

                dispatch(NotificationLog.builder()
                        .type(NotificationType.SALES_ORDER_DISPATCHED)
                        .partId(event.getSalesOrderId())
                        .partName(event.getOrderNumber())
                        .batchNumber(event.getDispatchNumber())
                        .message(message)
                        .actorUsername(event.getDispatchedBy())
                        .location(null)
                        .severity("SUCCESS")
                        .build(), message);
        }
}