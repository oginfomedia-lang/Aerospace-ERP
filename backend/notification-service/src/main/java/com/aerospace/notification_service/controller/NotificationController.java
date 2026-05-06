package com.aerospace.notification_service.controller;

import com.aerospace.notification_service.common.ApiResponse;
import com.aerospace.notification_service.entity.NotificationLog;
import com.aerospace.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/v1/notifications/recent
     * Last 20 notifications across all types — for the dashboard feed.
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getRecent() {
        return ResponseEntity.ok(
            new ApiResponse<>("SUCCESS", notificationService.getRecent()));
    }

    /**
     * GET /api/v1/notifications/part/{partId}
     * All notifications for a specific part — for the part detail page.
     */
    @GetMapping("/part/{partId}")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getByPart(
            @PathVariable Long partId) {
        return ResponseEntity.ok(
            new ApiResponse<>("SUCCESS", notificationService.getByPartId(partId)));
    }

    /**
     * GET /api/v1/notifications/batch/{batchNumber}
     * All notifications for a specific batch.
     */
    @GetMapping("/batch/{batchNumber}")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getByBatch(
            @PathVariable String batchNumber) {
        return ResponseEntity.ok(
            new ApiResponse<>("SUCCESS", notificationService.getByBatchNumber(batchNumber)));
    }

    /**
     * GET /api/v1/notifications/type/{type}
     * Filter by type: LOW_STOCK | QA_APPROVED | QA_REJECTED | QA_PENDING |
     *                 BATCH_EXPIRED | STOCK_RECEIVED | MATERIAL_ISSUED | PART_CREATED
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getByType(
            @PathVariable String type) {
        return ResponseEntity.ok(
            new ApiResponse<>("SUCCESS", notificationService.getByType(type)));
    }

    /**
     * GET /api/v1/notifications/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "notification-service UP"));
    }
}