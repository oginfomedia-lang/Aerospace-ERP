package com.aerospace.procurement_service.event;

public class KafkaTopics {
    // Consumed from inventory-service — these string values must exactly match
    // inventory-service's own KafkaTopics constants, since topics are just names,
    // not shared classes, in this Kafka-only (no cross-service REST) codebase.
    public static final String STOCK_LOW           = "stock-low-events";
    public static final String MATERIAL_SHORTAGE   = "material-shortage-events";
    public static final String QA_STATUS_CHANGED   = "qa-status-changed-events";
    // Published by inventory-service when an NCR is dispositioned RETURN_TO_VENDOR.
    public static final String VENDOR_RETURN = "vendor-return-events";

    public static final String RFQ_CREATED         = "rfq-created-events";
    public static final String QUOTATION_AWARDED   = "quotation-awarded-events";
    public static final String QUOTATION_REJECTED  = "quotation-rejected-events";
}
