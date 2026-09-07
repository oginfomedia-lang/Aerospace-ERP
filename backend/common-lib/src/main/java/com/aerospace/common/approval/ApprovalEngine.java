package com.aerospace.common.approval;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface ApprovalEngine {
    // Call this when a document is submitted. Returns the chain it created (empty = no
    // approval needed, caller should auto-approve).
    List<ApprovalHistory> submitForApproval(String moduleCode, Long documentId, BigDecimal amount, int documentVersion);

    // documentCreatedBy is the document's own recorded creator/submitter — pass null only
    // for legacy documents where no creator was ever tracked. When non-null and it matches
    // approverUsername, the approval is rejected: segregation of duties requires that the
    // person who created/submitted a document cannot also be the one who approves it.
    ApprovalOutcome approve(String moduleCode, Long documentId, String approverUsername, Set<String> approverRoles, String remarks, String documentCreatedBy);

    ApprovalOutcome reject(String moduleCode, Long documentId, String approverUsername, Set<String> approverRoles, String remarks);

    List<ApprovalHistory> getHistory(String moduleCode, Long documentId);

    // Finds every PENDING row whose band has escalationHours configured and that's
    // been sitting longer than that — reassigns (or just flags) it. Returns the rows
    // that were escalated this run, for the caller to log/notify.
    List<ApprovalHistory> runEscalationSweep();

    // Recomputes the digital signature hash from this row's own currently-stored fields
    // and compares it to what was recorded at the moment it was signed — the actual
    // tamper check the hash exists for (previously write-only, never re-verified anywhere).
    SignatureVerificationResult verifySignature(Long historyId);
}