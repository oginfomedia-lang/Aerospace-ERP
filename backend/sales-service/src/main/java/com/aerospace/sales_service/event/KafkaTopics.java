package com.aerospace.sales_service.event;

public class KafkaTopics {
    public static final String SALES_ORDER_DISPATCHED = "sales-order-dispatched-events";

    // Consumed by finance-service to build its local, read-only customer reference
    // mirror (same pattern as procurement-service's supplier-registered/updated events).
    public static final String CUSTOMER_REGISTERED = "customer-registered-events";
    public static final String CUSTOMER_UPDATED = "customer-updated-events";
}
