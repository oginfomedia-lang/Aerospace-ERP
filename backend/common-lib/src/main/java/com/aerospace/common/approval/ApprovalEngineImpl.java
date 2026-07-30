package com.aerospace.common.approval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApprovalEngineImpl implements ApprovalEngine {

    private final ApprovalMatrixRepository matrixRepository;
    private final ApprovalHistoryRepository historyRepository;

    @Override
    @Transactional
    public List<ApprovalHistory> submitForApproval(String moduleCode, Long documentId, BigDecimal amount, int documentVersion) {
        List<ApprovalMatrix> bands = matrixRepository
                .findByModuleCodeAndIsActiveTrueAndAmountFromLessThanEqualOrderByLevelSequenceAsc(moduleCode, amount);

        List<ApprovalHistory> rows = new ArrayList<>();
        for (int i = 0; i < bands.size(); i++) {
            ApprovalMatrix band = bands.get(i);
            rows.add(historyRepository.save(ApprovalHistory.builder()
                    .moduleCode(moduleCode)
                    .documentId(documentId)
                    .approvalLevel(band.getLevelSequence())
                    .approverRole(band.getApproverRole())
                    .status(i == 0 ? ApprovalActionStatus.PENDING : ApprovalActionStatus.WAITING)
                    .pendingSince(i == 0 ? LocalDateTime.now() : null)
                    .documentVersion(documentVersion)
                    .build()));
        }
        return rows;
    }

    @Override
    @Transactional
    public ApprovalOutcome approve(String moduleCode, Long documentId, String approverUsername,
                                    Set<String> approverRoles, String remarks) {
        List<ApprovalHistory> rows = historyRepository
                .findByModuleCodeAndDocumentIdOrderByApprovalLevelAsc(moduleCode, documentId);

        ApprovalHistory pending = rows.stream()
                .filter(r -> r.getStatus() == ApprovalActionStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No pending approval found for " + moduleCode + " #" + documentId));

        if (!approverRoles.contains(pending.getApproverRole())) {
            throw new IllegalArgumentException("This document requires role " + pending.getApproverRole()
                    + " to approve at level " + pending.getApprovalLevel() + ".");
        }

        LocalDateTime now = LocalDateTime.now();
        pending.setStatus(ApprovalActionStatus.APPROVED);
        pending.setApproverUsername(approverUsername);
        pending.setApprovedAt(now);
        pending.setRemarks(remarks);
        pending.setDigitalSignatureHash(computeSignatureHash(approverUsername, now, documentId, pending.getDocumentVersion(), remarks));
        historyRepository.save(pending);

        ApprovalHistory next = rows.stream()
                .filter(r -> r.getApprovalLevel() == pending.getApprovalLevel() + 1)
                .findFirst().orElse(null);

        if (next == null) {
            return new ApprovalOutcome(ApprovalOutcome.Status.FULLY_APPROVED, null, null);
        }
        next.setStatus(ApprovalActionStatus.PENDING);
        next.setPendingSince(LocalDateTime.now());
        historyRepository.save(next);
        return new ApprovalOutcome(ApprovalOutcome.Status.PENDING_NEXT_LEVEL, next.getApprovalLevel(), next.getApproverRole());
    }

    @Override
    @Transactional
    public ApprovalOutcome reject(String moduleCode, Long documentId, String approverUsername,
                                   Set<String> approverRoles, String remarks) {
        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException("Rejection remarks are mandatory.");
        }
        List<ApprovalHistory> rows = historyRepository
                .findByModuleCodeAndDocumentIdOrderByApprovalLevelAsc(moduleCode, documentId);

        ApprovalHistory pending = rows.stream()
                .filter(r -> r.getStatus() == ApprovalActionStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No pending approval found for " + moduleCode + " #" + documentId));

        if (!approverRoles.contains(pending.getApproverRole())) {
            throw new IllegalArgumentException("This document requires role " + pending.getApproverRole()
                    + " to reject at level " + pending.getApprovalLevel() + ".");
        }

        LocalDateTime now = LocalDateTime.now();
        pending.setStatus(ApprovalActionStatus.REJECTED);
        pending.setApproverUsername(approverUsername);
        pending.setApprovedAt(now);
        pending.setRemarks(remarks);
        pending.setDigitalSignatureHash(computeSignatureHash(approverUsername, now, documentId, pending.getDocumentVersion(), remarks));
        historyRepository.save(pending);
        return new ApprovalOutcome(ApprovalOutcome.Status.REJECTED, null, null);
    }

    @Override
    public List<ApprovalHistory> getHistory(String moduleCode, Long documentId) {
        return historyRepository.findByModuleCodeAndDocumentIdOrderByApprovalLevelAsc(moduleCode, documentId);
    }

    // Same tamper-evident technique every service used to duplicate on its own
    // (e.g. procurement-service's old PurchaseOrderServiceImpl.computeSignatureHash) —
    // now computed once, here, for every module that goes through this engine.
    private String computeSignatureHash(String actor, LocalDateTime timestamp, Long documentId, int documentVersion, String remarks) {
        String payload = actor + "|" + timestamp + "|" + documentId + "|" + documentVersion + "|" + (remarks != null ? remarks : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    @Transactional
    public List<ApprovalHistory> runEscalationSweep() {
        LocalDateTime now = LocalDateTime.now();
        List<ApprovalHistory> pendingRows = historyRepository.findAllByStatusAndEscalatedFalse(ApprovalActionStatus.PENDING);
        List<ApprovalHistory> justEscalated = new ArrayList<>();

        for (ApprovalHistory row : pendingRows) {
            if (row.getPendingSince() == null) continue;

            List<ApprovalMatrix> bands = matrixRepository.findByModuleCodeOrderByLevelSequenceAsc(row.getModuleCode());
            ApprovalMatrix band = bands.stream()
                    .filter(b -> b.getLevelSequence() == row.getApprovalLevel())
                    .findFirst().orElse(null);
            if (band == null || band.getEscalationHours() == null) continue;

            long hoursPending = java.time.Duration.between(row.getPendingSince(), now).toHours();
            if (hoursPending < band.getEscalationHours()) continue;

            if (band.getEscalationToRole() != null && !band.getEscalationToRole().isBlank()) {
                row.setApproverRole(band.getEscalationToRole());
            }
            row.setEscalated(true);
            row.setEscalatedAt(now);
            historyRepository.save(row);
            justEscalated.add(row);
        }
        return justEscalated;
    }
}