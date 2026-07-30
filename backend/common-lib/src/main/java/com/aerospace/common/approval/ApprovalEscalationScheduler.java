package com.aerospace.common.approval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalEscalationScheduler {

    private final ApprovalEngine approvalEngine;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(cron = "0 0 * * * *")
    public void sweep() {
        List<ApprovalHistory> escalated = approvalEngine.runEscalationSweep();
        for (ApprovalHistory row : escalated) {
            log.warn("[ESCALATION] {} #{} level {} escalated to role {} (pending since {})",
                    row.getModuleCode(), row.getDocumentId(), row.getApprovalLevel(),
                    row.getApproverRole(), row.getPendingSince());

            try {
                kafkaTemplate.send(ApprovalKafkaTopics.APPROVAL_ESCALATED, row.getModuleCode() + "-" + row.getDocumentId(),
                        ApprovalEscalatedEvent.builder()
                                .moduleCode(row.getModuleCode())
                                .documentId(row.getDocumentId())
                                .approvalLevel(row.getApprovalLevel())
                                .newApproverRole(row.getApproverRole())
                                .pendingSince(row.getPendingSince())
                                .build());
            } catch (Exception ex) {
                log.warn("[KAFKA] APPROVAL_ESCALATED publish failed for {} #{}: {}",
                        row.getModuleCode(), row.getDocumentId(), ex.getMessage());
            }
        }
    }
}