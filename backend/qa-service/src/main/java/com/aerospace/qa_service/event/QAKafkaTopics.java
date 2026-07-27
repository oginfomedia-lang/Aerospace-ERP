package com.aerospace.qa_service.event;

public class QAKafkaTopics {

    // Topics this service PUBLISHES to
    // inventory-service InventoryConsumer listens on this topic
    public static final String COMPLIANCE_QA_RESULT = "compliance.qa.result";
    public static final String COMPLIANCE_FAI_COMPLETED = "compliance.fai.completed";

    // Topics this service CONSUMES from
    // inventory-service publishes this when new stock arrives (GRN received)
    public static final String INVENTORY_STOCK_RECEIVED = "inventory.created";

    // Calibration due/overdue alert — published by CalibrationDueScheduler,
    // consumed by notification-service
    public static final String CALIBRATION_DUE = "qa.calibration.due";
}