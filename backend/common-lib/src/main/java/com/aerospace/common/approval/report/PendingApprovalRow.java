package com.aerospace.common.approval.report;

import java.time.LocalDateTime;

import lombok.*;


@Data @Builder
public class PendingApprovalRow {
    String moduleCode; Long documentId; int documentVersion; int approvalLevel; String approverRole;
    LocalDateTime pendingSince; Long hoursPending; Integer escalationHours; boolean alreadyEscalated; boolean slaBreached;
}

