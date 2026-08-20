package com.aerospace.finance_service.entity;

public enum PeriodStatus {
    OPEN,    // postings allowed
    CLOSED,  // postings blocked; reopenable by FINANCE_MANAGER/ADMIN
    LOCKED   // postings blocked permanently; must be unlocked (ADMIN only) before reopening
}
