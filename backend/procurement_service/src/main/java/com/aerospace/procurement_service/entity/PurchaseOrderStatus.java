package com.aerospace.procurement_service.entity;

public enum PurchaseOrderStatus {
    DRAFT, //created, not yet submitted to supplier
    SUBMITTED, //sent to supplier, awaiting acknowledgement
    ACKNOWLEDGED, //Supplier confirmed the order
    SHIPPED, //supplier has dispatched goods
    PARTIALLY_RECEIVED, //some line items received, not all
    RECEIVED, //all goods received - triggers GRN in inventory
    CANCELLED, //cancelled before receipt
    CLOSED //fully processed and archived
}
