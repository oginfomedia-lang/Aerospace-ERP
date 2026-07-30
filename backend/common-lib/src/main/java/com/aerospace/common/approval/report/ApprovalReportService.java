package com.aerospace.common.approval.report;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aerospace.common.approval.ApprovalActionStatus;
import com.aerospace.common.approval.ApprovalHistory;
import com.aerospace.common.approval.ApprovalHistoryRepository;
import com.aerospace.common.approval.ApprovalMatrix;
import com.aerospace.common.approval.ApprovalMatrixRepository;
import com.aerospace.common.sla.SlaEvaluator;
import com.aerospace.common.sla.SlaStatus;

import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class ApprovalReportService {
    private final ApprovalHistoryRepository historyRepository;
    private final ApprovalMatrixRepository matrixRepository;
    private static final List<ApprovalActionStatus> TERMINAL =
            List.of(ApprovalActionStatus.APPROVED, ApprovalActionStatus.REJECTED);

    private ApprovalMatrix findBand(String moduleCode, int level) {
        return matrixRepository.findByModuleCodeOrderByLevelSequenceAsc(moduleCode).stream()
                .filter(b -> b.getLevelSequence() == level).findFirst().orElse(null);
    }

    public List<PendingApprovalRow> getPendingApprovals(String moduleCode) {
        List<ApprovalHistory> rows = (moduleCode == null || moduleCode.isBlank())
            ? historyRepository.findByStatusOrderByPendingSinceAsc(ApprovalActionStatus.PENDING)
            : historyRepository.findByModuleCodeAndStatusOrderByPendingSinceAsc(moduleCode, ApprovalActionStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        return rows.stream().map(r -> {
            ApprovalMatrix band = findBand(r.getModuleCode(), r.getApprovalLevel());
            Integer slaHours = band != null ? band.getEscalationHours() : null;
            SlaStatus sla = SlaEvaluator.evaluate(
                slaHours != null && r.getPendingSince() != null ? r.getPendingSince().plusHours(slaHours) : null, now);
            return PendingApprovalRow.builder()
                .moduleCode(r.getModuleCode()).documentId(r.getDocumentId()).documentVersion(r.getDocumentVersion())
                .approvalLevel(r.getApprovalLevel()).approverRole(r.getApproverRole()).pendingSince(r.getPendingSince())
                .hoursPending(r.getPendingSince() != null ? Duration.between(r.getPendingSince(), now).toHours() : null)
                .escalationHours(slaHours).alreadyEscalated(r.isEscalated())
                .slaBreached(slaHours != null && sla.overdue()).build();
        }).toList();
    }

    public SlaComplianceReport getSlaCompliance(String moduleCode, LocalDateTime from, LocalDateTime to) {
        List<ApprovalHistory> terminalRows = (moduleCode == null || moduleCode.isBlank())
            ? historyRepository.findByStatusInAndApprovedAtBetween(TERMINAL, from, to)
            : historyRepository.findByModuleCodeAndStatusInAndApprovedAtBetween(moduleCode, TERMINAL, from, to);
        int compliant = 0, breached = 0, noSla = 0;
        List<SlaComplianceDetailRow> details = new ArrayList<>();
        for (ApprovalHistory row : terminalRows) {
            ApprovalMatrix band = findBand(row.getModuleCode(), row.getApprovalLevel());
            if (band == null || band.getEscalationHours() == null) { noSla++; continue; }
            boolean ok = !row.isEscalated();
            if (ok) compliant++; else breached++;
            details.add(SlaComplianceDetailRow.builder()
                .moduleCode(row.getModuleCode()).documentId(row.getDocumentId()).documentVersion(row.getDocumentVersion())
                .approvalLevel(row.getApprovalLevel()).approverRole(row.getApproverRole())
                .escalationHours(band.getEscalationHours()).escalated(row.isEscalated()).compliant(ok).build());
        }
        int evaluated = compliant + breached;
        return SlaComplianceReport.builder().moduleCode(moduleCode).periodFrom(from).periodTo(to)
            .compliantCount(compliant).breachedCount(breached).noSlaConfiguredCount(noSla)
            .compliancePercentage(evaluated == 0 ? null : (compliant * 100.0) / evaluated).details(details).build();
    }

    public List<TurnaroundRow> getTurnaroundTimes(String moduleCode, LocalDateTime from, LocalDateTime to) {
        List<ApprovalHistory> terminalRows = (moduleCode == null || moduleCode.isBlank())
            ? historyRepository.findByStatusInAndApprovedAtBetween(TERMINAL, from, to)
            : historyRepository.findByModuleCodeAndStatusInAndApprovedAtBetween(moduleCode, TERMINAL, from, to);
        record ChainKey(String moduleCode, Long documentId, int documentVersion) {}
        Set<ChainKey> keys = terminalRows.stream()
            .map(r -> new ChainKey(r.getModuleCode(), r.getDocumentId(), r.getDocumentVersion())).collect(Collectors.toSet());
        List<TurnaroundRow> out = new ArrayList<>();
        for (ChainKey key : keys) {
            List<ApprovalHistory> chain = historyRepository.findByModuleCodeAndDocumentIdAndDocumentVersionOrderByApprovalLevelAsc(
                key.moduleCode(), key.documentId(), key.documentVersion());
            boolean stillOpen = chain.stream().anyMatch(r -> r.getStatus() == ApprovalActionStatus.WAITING || r.getStatus() == ApprovalActionStatus.PENDING);
            if (stillOpen) continue;
            LocalDateTime start = chain.stream().min(Comparator.comparingInt(ApprovalHistory::getApprovalLevel)).map(ApprovalHistory::getPendingSince).orElse(null);
            LocalDateTime end = chain.stream().map(ApprovalHistory::getApprovedAt).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
            if (start == null || end == null) continue;
            boolean rejected = chain.stream().anyMatch(r -> r.getStatus() == ApprovalActionStatus.REJECTED);
            out.add(TurnaroundRow.builder().moduleCode(key.moduleCode()).documentId(key.documentId()).documentVersion(key.documentVersion())
                .outcome(rejected ? "REJECTED" : "APPROVED").startedAt(start).completedAt(end)
                .turnaroundHours(Duration.between(start, end).toHours()).levelsInChain(chain.size()).build());
        }
        return out;
    }
}
