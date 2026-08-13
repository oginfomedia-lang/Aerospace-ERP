package com.aerospace.sales_service.entity;

public enum CustomerStatus {
    /** Customer is active and can place orders. */
    ACTIVE,

    /** Customer is inactive and cannot place orders. */
    INACTIVE,

    /**
     * Customer is on hold (for example a credit issue).
     * This blocks creation of new Sales Orders.
     */
    ON_HOLD
}