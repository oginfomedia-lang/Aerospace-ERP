package com.aerospace.common.sla;

// Plain result of an SLA check — no entity, no persistence. Any service can compute
// this from whatever due-date field it already has, without adopting a shared schema.
public record SlaStatus(
    boolean overdue, 
    long hoursOverdue, 
    long hoursUntilDue) {
}