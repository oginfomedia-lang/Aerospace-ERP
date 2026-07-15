package com.aerospace.notification_service.event;

public class NotificationTopics {
    // All topics this service CONSUMES — published by inventory-service
    public static final String STOCK_LOW         = "inventory.stock.low";
    public static final String QA_STATUS_UPDATED = "inventory.qa.status.updated";
    public static final String MATERIAL_ISSUED   = "inventory.material.issued";
    public static final String STOCK_RECEIVED    = "inventory.created";
    public static final String PART_CREATED        = "part-created-events";
    public static final String VENDOR_RETURN     = "vendor-return-events";
}