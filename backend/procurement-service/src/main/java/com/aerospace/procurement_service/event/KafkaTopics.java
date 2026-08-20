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
    // Published by inventory-service when a GRN is formed from a Material Receipt.
    public static final String MATERIAL_RECEIVED = "material-received-events";

    public static final String RFQ_CREATED         = "rfq-created-events";
    public static final String QUOTATION_AWARDED   = "quotation-awarded-events";
    public static final String QUOTATION_REJECTED  = "quotation-rejected-events";

    public static final String PR_STATUS_CHANGED   = "pr-status-events";
    public static final String PO_APPROVAL_STATUS  = "po-approval-status-events";

    // Published by procurement-service's own overdue-delivery sweep.
    public static final String PO_DELIVERY_OVERDUE = "po-delivery-overdue-events";

    public static final String SUPPLIER_REGISTERED = "supplier-registered-events";
    public static final String SUPPLIER_CONFIRMATION_DECIDED = "supplier-confirmation-decided-events";

    // Published when a Vendor Invoice's status flips to APPROVED — consumed by
    // finance-service's PostingEngine to raise the AP liability journal entry.
    public static final String VENDOR_INVOICE_APPROVED = "vendor-invoice-approved";
}
