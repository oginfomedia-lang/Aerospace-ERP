package com.aerospace.qa_service.service.impl;

import com.aerospace.qa_service.dto.InspectionRequestDTO;
import com.aerospace.qa_service.dto.InspectionResponseDTO;
import com.aerospace.qa_service.entity.InspectionReport;
import com.aerospace.qa_service.entity.QADecision;
import com.aerospace.qa_service.event.QAResultEvent;
import com.aerospace.qa_service.event.producer.QAProducer;
import com.aerospace.qa_service.exception.ResourceNotFoundException;
import com.aerospace.qa_service.repository.InspectionRepository;
import com.aerospace.qa_service.service.InspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRepository inspectionRepository;
    private final QAProducer qaProducer;

    @Override
    @Transactional
    public InspectionResponseDTO submitDecision(InspectionRequestDTO dto) {

        // Validate the approved/rejected split adds up to the batch's total pending quantity —
        // synchronously, here, rather than letting a bad split fail silently later inside the
        // async Kafka consumer on the inventory-service side.
        int approvedQty = dto.getApprovedQty();
        int rejectedQty = dto.getRejectedQty();
        if (approvedQty + rejectedQty != dto.getTotalQuantity()) {
            throw new RuntimeException("Approved qty (" + approvedQty + ") + rejected qty (" + rejectedQty
                    + ") must equal the batch's total quantity (" + dto.getTotalQuantity() + ").");
        }

        // Decision is derived from the split, not client-sent — all-approved, all-rejected,
        // or a genuine partial split.
        QADecision decision = rejectedQty == 0 ? QADecision.APPROVED
                : approvedQty == 0 ? QADecision.REJECTED
                : QADecision.PARTIALLY_APPROVED;

        // Step 1: Save inspection report to QA database
        // This is saved BEFORE Kafka publish — if Kafka fails, we still have the record
        // and can retry later via retryFailedPublications()
        InspectionReport report = InspectionReport.builder()
                .partId(dto.getPartId())
                .batchNumber(dto.getBatchNumber())
                .decision(decision)
                .approvedQty(approvedQty)
                .rejectedQty(rejectedQty)
                .criteria(dto.getCriteria())
                .remarks(dto.getRemarks())
                .certificateNumber(dto.getCertificateNumber())
                .inspectorName(dto.getInspectorName())
                .publishedToInventory(false) // will be set true after successful Kafka publish
                .build();

        InspectionReport saved = inspectionRepository.save(report);
        log.info("Inspection saved — ID: {}, partId: {}, batch: {}, decision: {} (approved={}, rejected={})",
                 saved.getId(), saved.getPartId(), saved.getBatchNumber(), saved.getDecision(),
                 saved.getApprovedQty(), saved.getRejectedQty());

        // Step 2: Build Kafka event — must match QAStatusUpdatedEvent in inventory-service
        QAResultEvent event = QAResultEvent.builder()
                .partId(saved.getPartId())
                .batchNumber(saved.getBatchNumber())
                .status(saved.getDecision().name()) // "APPROVED", "REJECTED", or "PARTIALLY_APPROVED"
                .approvedQty(saved.getApprovedQty())
                .rejectedQty(saved.getRejectedQty())
                .criteria(saved.getCriteria())
                .location(dto.getLocation())
                .remarks(saved.getRemarks())
                .certificateNumber(saved.getCertificateNumber())
                .inspectorName(saved.getInspectorName())
                .build();

        // Step 3: Publish to Kafka → inventory-service picks it up automatically
        boolean published = qaProducer.publishQAResult(event);

        // Step 4: Update DB record to reflect publish status
        if (published) {
            saved.setPublishedToInventory(true);
            saved.setPublishedAt(LocalDateTime.now());
            inspectionRepository.save(saved);
            log.info("QA result published to inventory for batch: {}", saved.getBatchNumber());
        } else {
            log.warn("Kafka publish failed for batch: {} — will retry later", saved.getBatchNumber());
        }

        return toDTO(saved);
    }

    @Override
    public List<InspectionResponseDTO> getByPartId(Long partId) {
        return inspectionRepository.findByPartIdOrderByInspectedAtDesc(partId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public List<InspectionResponseDTO> getByBatchNumber(String batchNumber) {
        return inspectionRepository.findByBatchNumberOrderByInspectedAtDesc(batchNumber)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public InspectionResponseDTO getById(Long id) {
        return inspectionRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found with ID: " + id));
    }

    // Called by scheduler — retries any records that failed to publish to Kafka
    // This ensures no QA decision is ever lost even if Kafka is temporarily down
    @Override
    @Transactional
    public void retryFailedPublications() {
        List<InspectionReport> failed = inspectionRepository.findByPublishedToInventoryFalse();

        if (failed.isEmpty()) return;

        log.info("Retrying {} failed QA publications...", failed.size());

        for (InspectionReport report : failed) {
            QAResultEvent event = QAResultEvent.builder()
                    .partId(report.getPartId())
                    .batchNumber(report.getBatchNumber())
                    .status(report.getDecision().name())
                    .approvedQty(report.getApprovedQty())
                    .rejectedQty(report.getRejectedQty())
                    .criteria(report.getCriteria())
                    .remarks(report.getRemarks())
                    .certificateNumber(report.getCertificateNumber())
                    .inspectorName(report.getInspectorName())
                    .build();

            boolean success = qaProducer.publishQAResult(event);
            if (success) {
                report.setPublishedToInventory(true);
                report.setPublishedAt(LocalDateTime.now());
                inspectionRepository.save(report);
                log.info("Retry successful for inspection ID: {}", report.getId());
            }
        }
    }

    // Maps entity to response DTO
    private InspectionResponseDTO toDTO(InspectionReport report) {
        return InspectionResponseDTO.builder()
                .id(report.getId())
                .partId(report.getPartId())
                .batchNumber(report.getBatchNumber())
                .decision(report.getDecision().name())
                .approvedQty(report.getApprovedQty())
                .rejectedQty(report.getRejectedQty())
                .criteria(report.getCriteria())
                .remarks(report.getRemarks())
                .certificateNumber(report.getCertificateNumber())
                .inspectorName(report.getInspectorName())
                .inspectedAt(report.getInspectedAt())
                .publishedToInventory(report.isPublishedToInventory())
                .build();
    }
}