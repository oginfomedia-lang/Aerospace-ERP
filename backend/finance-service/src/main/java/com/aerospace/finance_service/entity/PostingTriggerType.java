package com.aerospace.finance_service.entity;

public enum PostingTriggerType {
    PURCHASE_RECEIPT,
    SUPPLIER_INVOICE,
    VENDOR_INVOICE_APPROVED,   // wired end-to-end in Phase 9
    INVENTORY_ADJUSTMENT,
    MATERIAL_CONSUMPTION,
    FINISHED_GOODS_RECEIPT,
    SALES_DISPATCH_CONFIRMED, // wired end-to-end in Phase 9
    CUSTOMER_INVOICE,
    MAINTENANCE_COST,
    QUALITY_COST
}
