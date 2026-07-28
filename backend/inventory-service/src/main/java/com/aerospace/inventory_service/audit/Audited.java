package com.aerospace.inventory_service.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Drop this on any controller method whose result should be recorded in the audit
// trail — AuditAspect does the rest (actor, IP, timestamp, before/after snapshot).
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {
    String action();      // e.g. "CREATE", "UPDATE", "DISPOSITION"
    String entityType();  // e.g. "Part", "NCR", "MaterialReceipt"
}
