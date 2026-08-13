package com.aerospace.notification_service.util;

import com.aerospace.notification_service.entity.NotificationType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Which roles actually need to know about each kind of notification. MANAGER and
// ADMIN are added to every type below (not listed per-line) since they can already
// see everything else in this app — this map only decides who ELSE gets it, both
// for the live WebSocket popup (WebSocketService) and the saved history list
// (NotificationServiceImpl) — both read from this exact same list, so they never
// disagree on who's allowed to see what.
public class NotificationRoleMapping {

    private static final Set<String> ALWAYS = Set.of("MANAGER", "ADMIN");

    private static final Map<NotificationType, Set<String>> ROLES = Map.ofEntries(
            // Inventory
            Map.entry(NotificationType.LOW_STOCK,       Set.of("STORE_KEEPER", "PROCUREMENT_OFFICER")),
            Map.entry(NotificationType.QA_APPROVED,     Set.of("QA_INSPECTOR", "STORE_KEEPER")),
            Map.entry(NotificationType.QA_REJECTED,     Set.of("QA_INSPECTOR", "STORE_KEEPER")),
            Map.entry(NotificationType.QA_PENDING,      Set.of("QA_INSPECTOR")),
            Map.entry(NotificationType.BATCH_EXPIRED,   Set.of("STORE_KEEPER", "QA_INSPECTOR")),
            Map.entry(NotificationType.STOCK_RECEIVED,  Set.of("STORE_KEEPER", "QA_INSPECTOR")),
            Map.entry(NotificationType.EXPIRY_WARNING,  Set.of("STORE_KEEPER", "QA_INSPECTOR")),
            Map.entry(NotificationType.MATERIAL_ISSUED, Set.of("STORE_KEEPER", "PRODUCTION_ENGINEER")),
            Map.entry(NotificationType.PART_CREATED,    Set.of("STORE_KEEPER", "PROCUREMENT_OFFICER")),

            // Work Orders
            Map.entry(NotificationType.WORK_ORDER_CREATED,   Set.of("PRODUCTION_ENGINEER", "PRODUCTION_SUPERVISOR")),
            Map.entry(NotificationType.WORK_ORDER_UPDATED,   Set.of("PRODUCTION_ENGINEER", "PRODUCTION_SUPERVISOR")),
            Map.entry(NotificationType.WORK_ORDER_COMPLETED, Set.of("PRODUCTION_ENGINEER", "PRODUCTION_SUPERVISOR")),
            Map.entry(NotificationType.VENDOR_RETURN,        Set.of("PROCUREMENT_OFFICER", "QA_INSPECTOR")),

            // QA / Compliance
            Map.entry(NotificationType.CALIBRATION_DUE,     Set.of("QA_INSPECTOR", "MAINTENANCE_ENGINEER")),
            Map.entry(NotificationType.FAI_COMPLETED,       Set.of("QA_INSPECTOR", "PRODUCTION_ENGINEER")),
            Map.entry(NotificationType.CAPA_STATUS_CHANGED, Set.of("QA_INSPECTOR")),
            Map.entry(NotificationType.CAPA_OVERDUE,        Set.of("QA_INSPECTOR")),

            // Approvals & Procurement
            Map.entry(NotificationType.APPROVAL_ESCALATED,  Set.of()), // MANAGER/ADMIN only, via ALWAYS
            Map.entry(NotificationType.PO_DELIVERY_OVERDUE, Set.of("PROCUREMENT_OFFICER")),
            Map.entry(NotificationType.PO_SENT_TO_VENDOR,   Set.of("PROCUREMENT_OFFICER")),

            // Suppliers
            Map.entry(NotificationType.SUPPLIER_PENDING_CONFIRMATION, Set.of("PROCUREMENT_OFFICER", "QA_INSPECTOR")),
            Map.entry(NotificationType.SUPPLIER_CONFIRMED,            Set.of("PROCUREMENT_OFFICER")),
            Map.entry(NotificationType.SUPPLIER_REJECTED,             Set.of("PROCUREMENT_OFFICER")),

            // Maintenance & Account
            Map.entry(NotificationType.MAINTENANCE_DUE,          Set.of("MAINTENANCE_ENGINEER", "PRODUCTION_SUPERVISOR")),
            Map.entry(NotificationType.PASSWORD_RESET_REQUESTED, Set.of()), // ADMIN only, via ALWAYS

            // Sales
            Map.entry(NotificationType.SALES_ORDER_DISPATCHED, Set.of("SALES_EXECUTIVE", "SALES_MANAGER", "WAREHOUSE_OPERATOR"))
    );

    // Every role that should see this notification type, including MANAGER/ADMIN.
    public static Set<String> rolesFor(NotificationType type) {
        Set<String> combined = new HashSet<>(ROLES.getOrDefault(type, Set.of()));
        combined.addAll(ALWAYS);
        return combined;
    }
}
