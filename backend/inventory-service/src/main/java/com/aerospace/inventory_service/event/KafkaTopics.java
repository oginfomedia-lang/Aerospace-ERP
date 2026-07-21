package com.aerospace.inventory_service.event;

public class KafkaTopics {
    // existing
    public static final String INVENTORY_CREATED     = "inventory.created";
    public static final String COMPLIANCE_QA_RESULT  = "compliance.qa.result";

    // ── ADD THESE — must match notification-service application.yml ──
    public static final String STOCK_RECEIVED        = "stock-received-events";
    public static final String PART_CREATED          = "part-created-events";
    public static final String MATERIAL_ISSUED       = "material-issued-events";
    public static final String STOCK_LOW             = "stock-low-events";
    public static final String QA_STATUS_CHANGED     = "qa-status-changed-events";

    // Consumed from order-service — must match order-service's own KafkaTopics constant.
    public static final String WORK_ORDER_CREATED    = "work-order-created-events";
    // Published by inventory-service, consumed by procurement-service.
    public static final String MATERIAL_SHORTAGE     = "material-shortage-events";

    // Published by inventory-service, consumed by procurement-service (auto-creates a Debit
    // Note against the supplier) and notification-service (alerts the team).
    public static final String VENDOR_RETURN = "vendor-return-events";

    //consumed from production-service - must watch production-service's own kafka topics constant
    public static final String PRODUCTION_ORDER_COMPLETED = "production-order-completed-events";
}