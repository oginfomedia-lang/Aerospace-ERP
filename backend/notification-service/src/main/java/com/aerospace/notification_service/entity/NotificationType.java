package com.aerospace.notification_service.entity;

public enum NotificationType {
    // ── Inventory events ──────────────────────────────────────────────────────
    LOW_STOCK,          // inventory quantity fell below reorder level
    QA_APPROVED,        // batch moved to APPROVED status
    QA_REJECTED,        // batch moved to REJECTED status
    QA_PENDING,         // batch waiting for inspection
    BATCH_EXPIRED,      // batch rejected because expiry date passed
    STOCK_RECEIVED,     // new GRN received, batch pending QA
    EXPIRY_WARNING,     // batch expiring within 30 days
    MATERIAL_ISSUED,    // stock issued to production
    PART_CREATED,       // new part registered in system

    // ── Work order events ─────────────────────────────────────────────────────
    WORK_ORDER_CREATED,   // new work order raised
    WORK_ORDER_UPDATED,   // work order status changed (non-terminal)
    WORK_ORDER_COMPLETED, // work order closed
    VENDOR_RETURN,        // NCR dispositioned RETURN_TO_VENDOR, auto-creates a Debit Note

    // ── QA events ────────────────────────────────────────────────────────────
    CALIBRATION_DUE,      // gauge/tool is due or overdue for calibration
    FAI_COMPLETED,        // First Article Inspection completed
    CAPA_STATUS_CHANGED,  // CAPA record stage updated
    CAPA_OVERDUE,

    APPROVAL_ESCALATED,
    PO_DELIVERY_OVERDUE,
    PO_SENT_TO_VENDOR,

    // ── Supplier onboarding events ────────────────────────────────────────────
    SUPPLIER_PENDING_CONFIRMATION, // new supplier added, waiting on QA/authority sign-off
    SUPPLIER_CONFIRMED,
    SUPPLIER_REJECTED
}