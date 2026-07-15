package com.aerospace.procurement_service.entity;

public enum DebitNoteStatus {
    OPEN,     // raised, not yet accounted for against any payment
    SETTLED   // manually marked as accounted for (deducted from a payment, credit note received, etc.)
}