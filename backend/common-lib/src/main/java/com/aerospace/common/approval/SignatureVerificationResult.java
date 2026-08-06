package com.aerospace.common.approval;

// Result of recomputing an ApprovalHistory row's tamper-evident hash from its own
// currently-stored fields and comparing it to what was recorded at signing time.
public record SignatureVerificationResult(boolean valid, String message) {
}
