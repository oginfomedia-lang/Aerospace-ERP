package com.aerospace.production_service.event;

public class KafkaTopics {
    // ── Topics this service PRODUCES ──────────────────────────────────────────
    public static final String PRODUCTION_ORDER_CREATED   = "production-order-created-events";
    public static final String PRODUCTION_ORDER_STARTED   = "production-order-started-events";
    public static final String PRODUCTION_ORDER_COMPLETED = "production-order-completed-events";
    public static final String PRODUCTION_ORDER_ON_HOLD   = "production-order-on-hold-events";
    public static final String PRODUCTION_ORDER_CANCELLED = "production-order-cancelled-events";
    public static final String STAGE_LOG_ADDED            = "production-stage-log-events";

    // ── Topics this service CONSUMES (from inventory-service) ─────────────────
    public static final String MATERIAL_ISSUED            = "material-issued-events";
    public static final String STOCK_LOW                  = "stock-low-events";
}
