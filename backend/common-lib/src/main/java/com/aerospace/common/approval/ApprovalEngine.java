package com.aerospace.common.approval;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface ApprovalEngine {
    // Call this when a document is submitted. Returns the chain it created (empty = no
    // approval needed, caller should auto-approve).
    List<ApprovalHistory> submitForApproval(String moduleCode, Long documentId, BigDecimal amount, int documentVersion);

    ApprovalOutcome approve(String moduleCode, Long documentId, String approverUsername, Set<String> approverRoles, String remarks);

    ApprovalOutcome reject(String moduleCode, Long documentId, String approverUsername, Set<String> approverRoles, String remarks);

    List<ApprovalHistory> getHistory(String moduleCode, Long documentId);

    // Finds every PENDING row whose band has escalationHours configured and that's
    // been sitting longer than that — reassigns (or just flags) it. Returns the rows
    // that were escalated this run, for the caller to log/notify.
    List<ApprovalHistory> runEscalationSweep();
}