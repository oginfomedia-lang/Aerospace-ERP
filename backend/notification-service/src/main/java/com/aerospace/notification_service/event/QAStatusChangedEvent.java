package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published by QA Service after an inspection decision is saved.
 * decision values: "APPROVED" | "REJECTED" | "PENDING_QA"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QAStatusChangedEvent {

    private Long partId;
    private String partName;
    private String batchNumber;
    private String previousStatus;
    private String newStatus;           // APPROVED | REJECTED | PENDING_QA
    private String decision;            // same as newStatus — kept for compatibility
    private String remarks;
    private String inspectorName;
    private String location;
    private boolean batchExpired;
    private String expiryDate;          // ISO date string, present when batchExpired=true
    private LocalDateTime changedAt;
}