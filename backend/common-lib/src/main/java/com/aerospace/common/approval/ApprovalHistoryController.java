package com.aerospace.common.approval;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Generic — works for any module's approval history row (PO, PR, WO, ...), since
// ApprovalHistory itself isn't module-specific.
@RestController
@RequestMapping("/api/approval-history")
@RequiredArgsConstructor
public class ApprovalHistoryController {

    private final ApprovalEngine approvalEngine;

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/{id}/verify-signature")
    public ResponseEntity<SignatureVerificationResult> verifySignature(@PathVariable Long id) {
        return ResponseEntity.ok(approvalEngine.verifySignature(id));
    }
}
