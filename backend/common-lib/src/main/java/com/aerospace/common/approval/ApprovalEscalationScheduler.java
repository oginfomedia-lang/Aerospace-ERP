package com.aerospace.common.approval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// Runs inside ANY service that depends on common-lib and has @EnableScheduling on its
// main class. Each service only sees its OWN database's approval_history rows (every
// service has its own DB), so this naturally never double-processes another service's
// data — it just quietly does nothing where there's no approval history yet.
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalEscalationScheduler {

    private final ApprovalEngine approvalEngine;

    // Hourly is enough resolution for a business-hours approval chain; adjust via the
    // cron string here if a tighter/looser check is ever needed.
    @Scheduled(cron = "0 0 * * * *")
    public void sweep() {
        List<ApprovalHistory> escalated = approvalEngine.runEscalationSweep();
        for (ApprovalHistory row : escalated) {
            log.warn("[ESCALATION] {} #{} level {} escalated to role {} (pending since {})",
                    row.getModuleCode(), row.getDocumentId(), row.getApprovalLevel(),
                    row.getApproverRole(), row.getPendingSince());
        }
    }
}