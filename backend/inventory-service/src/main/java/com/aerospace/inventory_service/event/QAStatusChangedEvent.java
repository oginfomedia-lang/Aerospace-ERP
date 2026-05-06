package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAStatusChangedEvent {
    // Legacy field — kept for existing sendQAStatusChangedEvent(Long, String, String) overload
    private String status;

    // ── Fields required by notification-service ──────────────────────────────
    private Long partId;
    private String partName;
    private String batchNumber;
    private String newStatus;
    private String decision;       // same value as newStatus — notification-service reads both
    private String inspectorName;
    private String location;
    private String remarks;
    private boolean batchExpired;
    private String expiryDate;
}