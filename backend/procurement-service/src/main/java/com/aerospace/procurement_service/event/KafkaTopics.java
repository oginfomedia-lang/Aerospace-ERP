package com.aerospace.procurement_service.event;

public class KafkaTopics {
    // Consumed from inventory-service — these string values must exactly match
    // inventory-service's own KafkaTopics constants, since topics are just names,
    // not shared classes, in this Kafka-only (no cross-service REST) codebase.
    public static final String STOCK_LOW         = "stock-low-events";
    public static final String MATERIAL_SHORTAGE = "material-shortage-events";
}
