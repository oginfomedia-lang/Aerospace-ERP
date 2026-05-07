package com.aerospace.order_service.event;

public class KafkaTopics {
    // Published by work-order-service
    public static final String WORK_ORDER_CREATED   = "work-order-created-events";
    public static final String WORK_ORDER_UPDATED   = "work-order-updated-events";
    public static final String WORK_ORDER_COMPLETED = "work-order-completed-events";
    public static final String MATERIAL_REQUEST     = "material-request-events";

    // Consumed from inventory-service
    public static final String QA_STATUS_CHANGED    = "qa-status-changed-events";
}