package com.aerospace.qa_service.service.impl;

import com.aerospace.qa_service.dto.FaiCharacteristicDTO;
import com.aerospace.qa_service.dto.FaiCreateRequestDTO;
import com.aerospace.qa_service.dto.FaiPerformRequestDTO;
import com.aerospace.qa_service.dto.FaiResponseDTO;
import com.aerospace.qa_service.entity.CharacteristicResult;
import com.aerospace.qa_service.entity.FaiCharacteristic;
import com.aerospace.qa_service.entity.FaiRecord;
import com.aerospace.qa_service.entity.FaiResult;
import com.aerospace.qa_service.entity.FaiStatus;
import com.aerospace.qa_service.exception.ResourceNotFoundException;
import com.aerospace.qa_service.repository.FaiRecordRepository;
import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.qa_service.service.FaiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaiServiceImpl implements FaiService {

    private final FaiRecordRepository faiRecordRepository;
    private final DocumentSequenceService documentSequenceService;
    private final com.aerospace.qa_service.event.producer.QAProducer qaProducer;

    @Override
    @Transactional
    public void autoQueueIfFirstArticle(Long partId, String partName, String skuCode) {
        if (faiRecordRepository.existsByPartId(partId)) {
            return; // already has an FAI (pending or complete) — nothing to do
        }
        FaiRecord record = FaiRecord.builder()
                .faiNumber(documentSequenceService.generate("FAI"))
                .partId(partId)
                .partName(partName)
                .skuCode(skuCode)
                .status(FaiStatus.PENDING)
                .build();
        faiRecordRepository.save(record);
        log.info("[FAI] Auto-queued {} for first article of partId={} ({})",
                record.getFaiNumber(), partId, partName);
    }

    @Override
    @Transactional
    public FaiResponseDTO createManual(FaiCreateRequestDTO dto) {
        if (faiRecordRepository.existsByPartId(dto.getPartId())) {
            throw new IllegalStateException("An FAI already exists for this part.");
        }
        FaiRecord record = FaiRecord.builder()
                .faiNumber(documentSequenceService.generate("FAI"))
                .partId(dto.getPartId())
                .partName(dto.getPartName())
                .skuCode(dto.getSkuCode())
                .status(FaiStatus.PENDING)
                .build();
        return toDTO(faiRecordRepository.save(record));
    }

    @Override
    public List<FaiResponseDTO> getPendingQueue() {
        List<FaiRecord> pending = faiRecordRepository.findByStatusOrderByCreatedAtDesc(FaiStatus.PENDING);
        List<FaiRecord> inProgress = faiRecordRepository.findByStatusOrderByCreatedAtDesc(FaiStatus.IN_PROGRESS);
        List<FaiRecord> all = new ArrayList<>(pending);
        all.addAll(inProgress);
        return all.stream().map(this::toDTO).toList();
    }

    @Override
    public FaiResponseDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    public List<FaiResponseDTO> getByPartId(Long partId) {
        return faiRecordRepository.findByPartIdOrderByCreatedAtDesc(partId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public FaiResponseDTO performFai(Long id, FaiPerformRequestDTO dto) {
        FaiRecord record = findOrThrow(id);
        if (record.getStatus() == FaiStatus.COMPLETE) {
            throw new IllegalStateException("FAI " + record.getFaiNumber() + " has already been completed.");
        }

        record.setBatchNumber(dto.getBatchNumber());
        record.setPartRevision(dto.getPartRevision());
        record.setMaterialCertRef(dto.getMaterialCertRef());
        record.setInspectorName(dto.getInspectorName());
        record.setEquipmentAssetTag(dto.getEquipmentAssetTag());
        record.setRemarks(dto.getRemarks());
        record.setOverallResult(FaiResult.valueOf(dto.getOverallResult()));
        record.setPerformedAt(LocalDateTime.now());
        record.setStatus(FaiStatus.COMPLETE);

        record.getCharacteristics().clear();
        for (FaiCharacteristicDTO c : dto.getCharacteristics()) {
            record.getCharacteristics().add(FaiCharacteristic.builder()
                    .faiRecord(record)
                    .charNo(c.getCharNo())
                    .description(c.getDescription())
                    .requirement(c.getRequirement())
                    .measuredResult(c.getMeasuredResult())
                    .result(c.getResult() != null ? CharacteristicResult.valueOf(c.getResult()) : null)
                    .remarks(c.getRemarks())
                    .build());
        }

        record.setSignatureHash(computeSignatureHash(
                record.getInspectorName(), record.getPerformedAt(), record.getId(), 1, record.getRemarks()));

        FaiRecord saved = faiRecordRepository.save(record);
        log.info("[FAI] {} completed — result: {}, characteristics: {}",
                saved.getFaiNumber(), saved.getOverallResult(), saved.getCharacteristics().size());

        // Publish FAI completion event to Kafka
        try {
            qaProducer.publishFaiResult(com.aerospace.qa_service.event.FaiResultEvent.builder()
                    .id(saved.getId())
                    .faiNumber(saved.getFaiNumber())
                    .partId(saved.getPartId())
                    .partName(saved.getPartName())
                    .skuCode(saved.getSkuCode())
                    .batchNumber(saved.getBatchNumber())
                    .partRevision(saved.getPartRevision())
                    .overallResult(saved.getOverallResult().name())
                    .inspectorName(saved.getInspectorName())
                    .equipmentAssetTag(saved.getEquipmentAssetTag())
                    .remarks(saved.getRemarks())
                    .performedAt(saved.getPerformedAt())
                    .build());
        } catch (Exception ex) {
            log.warn("[FAI] Failed to publish FAI completion event: {}", ex.getMessage());
        }

        return toDTO(saved);
    }

    private FaiRecord findOrThrow(Long id) {
        return faiRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAI record not found: " + id));
    }

    // Same technique as procurement-service's PurchaseOrderServiceImpl.computeSignatureHash
    private String computeSignatureHash(String actor, LocalDateTime timestamp, Long entityId, int version, String remarks) {
        try {
            String payload = actor + "|" + timestamp + "|" + entityId + "|" + version + "|" + (remarks != null ? remarks : "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private FaiResponseDTO toDTO(FaiRecord r) {
        List<FaiCharacteristicDTO> characteristics = r.getCharacteristics().stream()
                .map(c -> FaiCharacteristicDTO.builder()
                        .charNo(c.getCharNo())
                        .description(c.getDescription())
                        .requirement(c.getRequirement())
                        .measuredResult(c.getMeasuredResult())
                        .result(c.getResult() != null ? c.getResult().name() : null)
                        .remarks(c.getRemarks())
                        .build())
                .toList();

        return FaiResponseDTO.builder()
                .id(r.getId())
                .faiNumber(r.getFaiNumber())
                .partId(r.getPartId())
                .partName(r.getPartName())
                .skuCode(r.getSkuCode())
                .partRevision(r.getPartRevision())
                .batchNumber(r.getBatchNumber())
                .status(r.getStatus().name())
                .overallResult(r.getOverallResult() != null ? r.getOverallResult().name() : null)
                .materialCertRef(r.getMaterialCertRef())
                .inspectorName(r.getInspectorName())
                .equipmentAssetTag(r.getEquipmentAssetTag())
                .performedAt(r.getPerformedAt())
                .remarks(r.getRemarks())
                .signatureHash(r.getSignatureHash())
                .createdAt(r.getCreatedAt())
                .characteristics(characteristics)
                .build();
    }
}
