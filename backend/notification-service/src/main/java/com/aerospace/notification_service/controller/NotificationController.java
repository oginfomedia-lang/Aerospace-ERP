package com.aerospace.notification_service.controller;

import com.aerospace.notification_service.common.ApiResponse;
import com.aerospace.notification_service.entity.NotificationLog;
import com.aerospace.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@PreAuthorize("!hasRole('SUPPLIER')")
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/v1/notifications/recent
     * Last 20 notifications across all types the caller's own role(s) can see —
     * for the dashboard feed.
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getRecent(Authentication authentication) {
        return ResponseEntity.ok(
            new ApiResponse<>("SUCCESS", notificationService.getRecent(rolesOf(authentication))));
    }

    /**
     * GET /api/v1/notifications/part/{partId}
     * All notifications for a specific part — for the part detail page.
     */
    @GetMapping("/part/{partId}")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getByPart(
            @PathVariable Long partId, Authentication authentication) {
        return ResponseEntity.ok(
            new ApiResponse<>("SUCCESS", notificationService.getByPartId(partId, rolesOf(authentication))));
    }

    /**
     * GET /api/v1/notifications/batch/{batchNumber}
     * All notifications for a specific batch.
     */
    @GetMapping("/batch/{batchNumber}")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getByBatch(
            @PathVariable String batchNumber, Authentication authentication) {
        return ResponseEntity.ok(
            new ApiResponse<>("SUCCESS", notificationService.getByBatchNumber(batchNumber, rolesOf(authentication))));
    }

    /**
     * GET /api/v1/notifications/type/{type}
     * Filter by type: LOW_STOCK | QA_APPROVED | QA_REJECTED | QA_PENDING |
     *                 BATCH_EXPIRED | STOCK_RECEIVED | MATERIAL_ISSUED | PART_CREATED
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getByType(
            @PathVariable String type, Authentication authentication) {
        return ResponseEntity.ok(
            new ApiResponse<>("SUCCESS", notificationService.getByType(type, rolesOf(authentication))));
    }

    /**
     * GET /api/v1/notifications/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "notification-service UP"));
    }

    // Authentication's roles come back prefixed "ROLE_" (that's how @PreAuthorize
    // matches them) — NotificationRoleMapping wants the bare role name, so strip it
    // here, the same way SalesOrderController.rolesOf() already does.
    private Set<String> rolesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toSet());
    }
}
