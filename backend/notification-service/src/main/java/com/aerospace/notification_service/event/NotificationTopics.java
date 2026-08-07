package com.aerospace.notification_service.event;

public class NotificationTopics {
    // All topics this service CONSUMES — published by inventory-service
    public static final String STOCK_LOW         = "inventory.stock.low";
    public static final String QA_STATUS_UPDATED = "inventory.qa.status.updated";
    public static final String MATERIAL_ISSUED   = "inventory.material.issued";
    public static final String STOCK_RECEIVED    = "inventory.created";
    public static final String PART_CREATED        = "part-created-events";
    public static final String VENDOR_RETURN     = "vendor-return-events";

    // Published by qa-service
    public static final String CALIBRATION_DUE  = "qa.calibration.due";
    public static final String COMPLIANCE_FAI_COMPLETED = "compliance.fai.completed";
    public static final String CAPA_EVENT       = "inventory.capa.event";

    // Published by procurement-service's own overdue-delivery sweep.
    public static final String PO_DELIVERY_OVERDUE = "po-delivery-overdue-events";

    // Published by procurement-service when a PO is sent to vendor.
    public static final String PO_CREATED = "procurement-po-created";

    // Published by procurement-service when a supplier is added / confirmed / rejected.
    public static final String SUPPLIER_REGISTERED = "supplier-registered-events";
    public static final String SUPPLIER_CONFIRMATION_DECIDED = "supplier-confirmation-decided-events";

    public static final String MAINTENANCE_DUE = "production-resource.maintenance.due";
}