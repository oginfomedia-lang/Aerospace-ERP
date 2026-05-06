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
}