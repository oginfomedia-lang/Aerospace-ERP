package com.aerospace.finance_service.security;

/**
 * Thread-local store for the current request's tenant ID.
 * Set by JwtAuthenticationFilter at the start of every request;
 * cleared in the finally block so it never leaks between threads
 * (critical for container thread pools that reuse threads).
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /** Must be called in a finally block at the end of each request to avoid thread pool leaks. */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
