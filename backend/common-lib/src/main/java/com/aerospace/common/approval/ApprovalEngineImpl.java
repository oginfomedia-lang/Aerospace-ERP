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
import java.util.Optional;
import java.util.OptionalInt;
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

        // Bands sharing a levelSequence are parallel peers (Parallel Approval) — every band
        // at the FIRST distinct level starts PENDING together; everything at later levels
        // waits. With one band per level (today's only real config), this is identical to
        // the old "bands.get(0) is PENDING, rest WAITING" behavior.
        List<Integer> levelsInOrder = bands.stream().map(ApprovalMatrix::getLevelSequence).distinct().toList();
        int firstLevel = levelsInOrder.isEmpty() ? -1 : levelsInOrder.get(0);

        List<ApprovalHistory> rows = new ArrayList<>();
        for (ApprovalMatrix band : bands) {
            boolean isFirst = band.getLevelSequence() == firstLevel;
            rows.add(historyRepository.save(ApprovalHistory.builder()
                    .moduleCode(moduleCode)
                    .documentId(documentId)
                    .approvalLevel(band.getLevelSequence())
                    .approverRole(band.getApproverRole())
                    .status(isFirst ? ApprovalActionStatus.PENDING : ApprovalActionStatus.WAITING)
                    .pendingSince(isFirst ? LocalDateTime.now() : null)
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

        List<ApprovalHistory> pendingCandidates = rows.stream()
                .filter(r -> r.getStatus() == ApprovalActionStatus.PENDING)
                .toList();
        if (pendingCandidates.isEmpty()) {
            throw new IllegalStateException("No pending approval found for " + moduleCode + " #" + documentId);
        }
        ApprovalHistory pending = resolveActingRow(moduleCode, pendingCandidates, approverRoles, approverUsername)
                .orElseThrow(() -> new IllegalArgumentException(
                        "None of the pending approvals at this stage match your role/user."));

        LocalDateTime now = LocalDateTime.now();
        pending.setStatus(ApprovalActionStatus.APPROVED);
        pending.setApproverUsername(approverUsername);
        pending.setApprovedAt(now);
        pending.setRemarks(remarks);
        pending.setDigitalSignatureHash(computeSignatureHash(approverUsername, now, documentId, pending.getDocumentVersion(), remarks));
        historyRepository.save(pending);

        // Parallel Approval: don't advance until every peer sharing this level has APPROVED.
        // With one band per level (today's only real config) this is trivially true the
        // instant `pending` itself approves, same as before.
        int currentLevel = pending.getApprovalLevel();
        boolean allPeersCleared = rows.stream()
                .filter(r -> r.getApprovalLevel() == currentLevel)
                .allMatch(r -> r == pending || r.getStatus() == ApprovalActionStatus.APPROVED);

        if (!allPeersCleared) {
            return new ApprovalOutcome(ApprovalOutcome.Status.PENDING_NEXT_LEVEL, currentLevel, pending.getApproverRole());
        }

        // "Next distinct level above current" instead of the old hardcoded level+1 — this
        // also correctly handles non-contiguous level numbers (e.g. 1, 5, 10), which the
        // old +1 lookup would have silently mishandled.
        OptionalInt nextLevel = rows.stream()
                .filter(r -> r.getApprovalLevel() > currentLevel && r.getStatus() == ApprovalActionStatus.WAITING)
                .mapToInt(ApprovalHistory::getApprovalLevel)
                .min();

        if (nextLevel.isEmpty()) {
            return new ApprovalOutcome(ApprovalOutcome.Status.FULLY_APPROVED, null, null);
        }
        List<ApprovalHistory> promoted = rows.stream()
                .filter(r -> r.getApprovalLevel() == nextLevel.getAsInt() && r.getStatus() == ApprovalActionStatus.WAITING)
                .toList();
        promoted.forEach(r -> { r.setStatus(ApprovalActionStatus.PENDING); r.setPendingSince(now); });
        historyRepository.saveAll(promoted);

        String nextRole = promoted.stream().map(ApprovalHistory::getApproverRole).sorted().findFirst().orElse(null);
        return new ApprovalOutcome(ApprovalOutcome.Status.PENDING_NEXT_LEVEL, nextLevel.getAsInt(), nextRole);
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

        List<ApprovalHistory> pendingCandidates = rows.stream()
                .filter(r -> r.getStatus() == ApprovalActionStatus.PENDING)
                .toList();
        if (pendingCandidates.isEmpty()) {
            throw new IllegalStateException("No pending approval found for " + moduleCode + " #" + documentId);
        }
        ApprovalHistory pending = resolveActingRow(moduleCode, pendingCandidates, approverRoles, approverUsername)
                .orElseThrow(() -> new IllegalArgumentException(
                        "None of the pending approvals at this stage match your role/user."));

        LocalDateTime now = LocalDateTime.now();
        pending.setStatus(ApprovalActionStatus.REJECTED);
        pending.setApproverUsername(approverUsername);
        pending.setApprovedAt(now);
        pending.setRemarks(remarks);
        pending.setDigitalSignatureHash(computeSignatureHash(approverUsername, now, documentId, pending.getDocumentVersion(), remarks));
        historyRepository.save(pending);

        // Parallel Approval: a rejection kills the whole chain, so any sibling still PENDING
        // at this same level is now moot — close it out too instead of leaving it dangling.
        // For sequential (non-parallel) bands there is never a sibling, so this is a no-op.
        List<ApprovalHistory> siblings = rows.stream()
                .filter(r -> r != pending && r.getApprovalLevel() == pending.getApprovalLevel()
                        && r.getStatus() == ApprovalActionStatus.PENDING)
                .toList();
        siblings.forEach(r -> {
            r.setStatus(ApprovalActionStatus.REJECTED);
            r.setRemarks("Auto-closed: peer rejected at this level.");
        });
        if (!siblings.isEmpty()){ historyRepository.saveAll(siblings);}

        return new ApprovalOutcome(ApprovalOutcome.Status.REJECTED, null, null);
    }

    @Override
    public List<ApprovalHistory> getHistory(String moduleCode, Long documentId) {
        return historyRepository.findByModuleCodeAndDocumentIdOrderByApprovalLevelAsc(moduleCode, documentId);
    }

    // Which of possibly-several PENDING rows (parallel peers share a level) is THIS caller
    // allowed to act on: either they hold the row's configured role, or — if the originating
    // band pins a specific approverUsername (User-Based Approval) — they ARE that exact
    // user regardless of role. Bands never set approverUsername/parallelGroup today, so for
    // every existing PurchaseOrder/VendorInvoice band this reduces to "the one PENDING row,
    // if its role matches" — identical to the old single-row check.
    private Optional<ApprovalHistory> resolveActingRow(String moduleCode, List<ApprovalHistory> pendingCandidates,
                                                        Set<String> approverRoles, String approverUsername) {
        List<ApprovalMatrix> bands = matrixRepository.findByModuleCodeOrderByLevelSequenceAsc(moduleCode);
        for (ApprovalHistory row : pendingCandidates) {
            ApprovalMatrix band = bands.stream()
                    .filter(b -> b.getLevelSequence() == row.getApprovalLevel()
                            && b.getApproverRole().equals(row.getApproverRole()))
                    .findFirst().orElse(null);
            boolean roleMatches = approverRoles.contains(row.getApproverRole());
            boolean userMatches = band != null && band.getApproverUsername() != null
                    && band.getApproverUsername().equalsIgnoreCase(approverUsername);
            if (roleMatches || userMatches) return Optional.of(row);
        }
        return Optional.empty();
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