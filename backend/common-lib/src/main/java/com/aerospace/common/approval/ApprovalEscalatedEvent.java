package com.aerospace.common.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Published once per row runEscalationSweep() actually escalates. notification-service
// keeps its own local mirror of this (matched by JSON field name, same convention as
// every other cross-service event in this codebase — see QAStatusChangedEvent).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalEscalatedEvent {
    private String moduleCode;
    private Long documentId;
    private int approvalLevel;
    private String newApproverRole;
    private LocalDateTime pendingSince;
}