package com.aerospace.inventory_service.service.impl;

import com.aerospace.inventory_service.dto.CapaHistoryDTO;
import com.aerospace.inventory_service.dto.CapaOpenRequest;
import com.aerospace.inventory_service.dto.CapaResponse;
import com.aerospace.inventory_service.dto.CapaTransitionRequest;
import com.aerospace.inventory_service.entity.CapaHistory;
import com.aerospace.inventory_service.entity.CapaRecord;
import com.aerospace.inventory_service.entity.CapaStatus;
import com.aerospace.inventory_service.entity.CapaType;
import com.aerospace.inventory_service.entity.NonConformanceReport;
import com.aerospace.inventory_service.exception.ResourceNotFoundException;
import com.aerospace.inventory_service.repository.CapaHistoryRepository;
import com.aerospace.inventory_service.repository.CapaRepository;
import com.aerospace.inventory_service.repository.NcrRepository;
import com.aerospace.inventory_service.service.CapaService;
import com.aerospace.inventory_service.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CapaServiceImpl implements CapaService {

    private static final List<CapaStatus> TERMINAL = List.of(CapaStatus.CLOSED, CapaStatus.REJECTED);

    private final CapaRepository capaRepository;
    private final CapaHistoryRepository historyRepository;
    private final NcrRepository ncrRepository;
    private final DocumentSequenceService documentSequenceService;
    private final com.aerospace.inventory_service.event.producer.InventoryProducer inventoryProducer;

    @Override
    @Transactional
    public CapaResponse openCapa(Long ncrId, CapaOpenRequest request) {
        NonConformanceReport ncr = ncrRepository.findById(ncrId)
                .orElseThrow(() -> new ResourceNotFoundException("NCR not found: " + ncrId));

        boolean hasActive = capaRepository.findByNcrIdOrderByCreatedAtDesc(ncrId).stream()
                .anyMatch(c -> !TERMINAL.contains(c.getStatus()));
        if (hasActive) {
            throw new IllegalStateException("An active CAPA already exists for NCR " + ncr.getNcrNumber() + ".");
        }

        CapaRecord capa = CapaRecord.builder()
                .capaNumber(documentSequenceService.generate("CAPA"))
                .ncr(ncr)
                .capaType(CapaType.valueOf(request.getCapaType()))
                .status(CapaStatus.OPEN)
                .ownerName(request.getOwnerName())
                .targetCloseDate(request.getTargetCloseDate())
                .createdBy(request.getCreatedBy())
                .build();
        CapaRecord saved = capaRepository.save(capa);

        appendHistory(saved, null, CapaStatus.OPEN, request.getCreatedBy(), request.getRemarks());
        log.info("[CAPA] {} opened against NCR {}", saved.getCapaNumber(), ncr.getNcrNumber());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public CapaResponse logRootCause(Long id, CapaTransitionRequest request) {
        if (request.getRootCause() == null || request.getRootCause().isBlank()) {
            throw new IllegalArgumentException("Root cause is required for this stage.");
        }
        CapaRecord capa = transition(id, CapaStatus.OPEN, CapaStatus.ROOT_CAUSE_LOGGED, request);
        capa.setRootCause(request.getRootCause());
        return toDTO(capaRepository.save(capa));
    }

    @Override
    @Transactional
    public CapaResponse planActions(Long id, CapaTransitionRequest request) {
        if (request.getCorrectiveAction() == null || request.getCorrectiveAction().isBlank()) {
            throw new IllegalArgumentException("Corrective action is required for this stage.");
        }
        CapaRecord capa = transition(id, CapaStatus.ROOT_CAUSE_LOGGED, CapaStatus.ACTION_PLANNED, request);
        capa.setCorrectiveAction(request.getCorrectiveAction());
        capa.setPreventiveAction(request.getPreventiveAction());
        return toDTO(capaRepository.save(capa));
    }

    @Override
    @Transactional
    public CapaResponse startImplementation(Long id, CapaTransitionRequest request) {
        CapaRecord capa = transition(id, CapaStatus.ACTION_PLANNED, CapaStatus.IN_PROGRESS, request);
        return toDTO(capaRepository.save(capa));
    }

    @Override
    @Transactional
    public CapaResponse submitForVerification(Long id, CapaTransitionRequest request) {
        CapaRecord capa = transition(id, CapaStatus.IN_PROGRESS, CapaStatus.PENDING_VERIFICATION, request);
        return toDTO(capaRepository.save(capa));
    }

    @Override
    @Transactional
    public CapaResponse verifyAndClose(Long id, CapaTransitionRequest request) {
        if (request.getVerificationRemarks() == null || request.getVerificationRemarks().isBlank()) {
            throw new IllegalArgumentException("Verification remarks are required to close a CAPA.");
        }
        CapaRecord capa = transition(id, CapaStatus.PENDING_VERIFICATION, CapaStatus.CLOSED, request);
        capa.setVerificationRemarks(request.getVerificationRemarks());
        capa.setVerifiedBy(request.getActor());
        capa.setVerifiedAt(LocalDateTime.now());
        capa.setClosedAt(LocalDateTime.now());
        return toDTO(capaRepository.save(capa));
    }

    @Override
    @Transactional
    public CapaResponse reject(Long id, CapaTransitionRequest request) {
        CapaRecord capa = findOrThrow(id);
        if (TERMINAL.contains(capa.getStatus())) {
            throw new IllegalStateException("CAPA " + capa.getCapaNumber() + " is already " + capa.getStatus() + ".");
        }
        CapaStatus from = capa.getStatus();
        capa.setStatus(CapaStatus.REJECTED);
        CapaRecord saved = capaRepository.save(capa);
        appendHistory(saved, from, CapaStatus.REJECTED, request.getActor(), request.getRemarks());
        return toDTO(saved);
    }

    @Override
    public CapaResponse getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    public List<CapaResponse> getByNcr(Long ncrId) {
        return capaRepository.findByNcrIdOrderByCreatedAtDesc(ncrId).stream().map(this::toDTO).toList();
    }

    @Override
    public List<CapaResponse> getAll(String status) {
        List<CapaRecord> records = (status != null && !status.isBlank())
                ? capaRepository.findByStatus(CapaStatus.valueOf(status.toUpperCase()))
                : capaRepository.findAllByOrderByCreatedAtDesc();
        return records.stream().map(this::toDTO).toList();
    }

    // Shared "advance one stage" logic: validates the CAPA is currently in `expectedCurrent`,
    // moves it to `next`, and appends the history row. Callers set stage-specific fields
    // and call capaRepository.save(...) themselves afterward (mirrors NcrServiceImpl.disposition's
    // "mutate then save" shape).
    private CapaRecord transition(Long id, CapaStatus expectedCurrent, CapaStatus next, CapaTransitionRequest request) {
        CapaRecord capa = findOrThrow(id);
        if (capa.getStatus() != expectedCurrent) {
            throw new IllegalStateException("CAPA " + capa.getCapaNumber() + " must be " + expectedCurrent
                    + " for this action — it is currently " + capa.getStatus() + ".");
        }
        capa.setStatus(next);
        appendHistory(capa, expectedCurrent, next, request.getActor(), request.getRemarks());
        return capa;
    }

    private void appendHistory(CapaRecord capa, CapaStatus from, CapaStatus to, String actor, String remarks) {
        LocalDateTime now = LocalDateTime.now();
        historyRepository.save(CapaHistory.builder()
                .capaRecord(capa)
                .fromStatus(from)
                .toStatus(to)
                .actor(actor)
                .remarks(remarks)
                .signatureHash(computeSignatureHash(actor, now, capa.getId(), remarks))
                .build());

        try {
            inventoryProducer.sendCapaEvent(com.aerospace.inventory_service.event.CapaEvent.builder()
                    .id(capa.getId())
                    .capaNumber(capa.getCapaNumber())
                    .ncrId(capa.getNcr() != null ? capa.getNcr().getId() : null)
                    .ncrNumber(capa.getNcr() != null ? capa.getNcr().getNcrNumber() : null)
                    .partName(capa.getNcr() != null ? capa.getNcr().getPartName() : null)
                    .batchNumber(capa.getNcr() != null ? capa.getNcr().getBatchNumber() : null)
                    .capaType(capa.getCapaType() != null ? capa.getCapaType().name() : null)
                    .fromStatus(from != null ? from.name() : null)
                    .toStatus(to.name())
                    .actor(actor)
                    .remarks(remarks)
                    .timestamp(now)
                    .build());
        } catch (Exception ex) {
            log.warn("[CAPA] Failed to publish CAPA event for {}: {}", capa.getCapaNumber(), ex.getMessage());
        }
    }

    private CapaRecord findOrThrow(Long id) {
        return capaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CAPA not found: " + id));
    }

    // Same technique as procurement-service's PurchaseOrderServiceImpl.computeSignatureHash.
    // CAPA has no document-version concept, so version is always 1.
    private String computeSignatureHash(String actor, LocalDateTime timestamp, Long entityId, String remarks) {
        try {
            String payload = actor + "|" + timestamp + "|" + entityId + "|1|" + (remarks != null ? remarks : "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private CapaResponse toDTO(CapaRecord c) {
        List<CapaHistoryDTO> history = historyRepository.findByCapaRecordIdOrderByOccurredAtAsc(c.getId()).stream()
                .map(h -> CapaHistoryDTO.builder()
                        .fromStatus(h.getFromStatus() != null ? h.getFromStatus().name() : null)
                        .toStatus(h.getToStatus().name())
                        .actor(h.getActor())
                        .remarks(h.getRemarks())
                        .signatureHash(h.getSignatureHash())
                        .occurredAt(h.getOccurredAt())
                        .build())
                .toList();

        return CapaResponse.builder()
                .id(c.getId())
                .capaNumber(c.getCapaNumber())
                .ncrId(c.getNcr().getId())
                .ncrNumber(c.getNcr().getNcrNumber())
                .ncrPartName(c.getNcr().getPartName())
                .ncrBatchNumber(c.getNcr().getBatchNumber())
                .capaType(c.getCapaType().name())
                .status(c.getStatus().name())
                .rootCause(c.getRootCause())
                .correctiveAction(c.getCorrectiveAction())
                .preventiveAction(c.getPreventiveAction())
                .targetCloseDate(c.getTargetCloseDate())
                .ownerName(c.getOwnerName())
                .verificationRemarks(c.getVerificationRemarks())
                .verifiedBy(c.getVerifiedBy())
                .verifiedAt(c.getVerifiedAt())
                .closedAt(c.getClosedAt())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .history(history)
                .build();
    }
}
