package com.aerospace.inventory_service.event.consumer;

import com.aerospace.inventory_service.entity.NcrStatus;
import com.aerospace.inventory_service.entity.NonConformanceReport;
import com.aerospace.inventory_service.event.FaiResultEvent;
import com.aerospace.inventory_service.event.KafkaTopics;
import com.aerospace.inventory_service.event.PartCreatedEvent;
import com.aerospace.inventory_service.event.QAStatusUpdatedEvent;
import com.aerospace.inventory_service.repository.NcrRepository;
import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.inventory_service.service.PartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryConsumer {

    private final PartService partService;
    private final NcrRepository ncrRepository;
    private final DocumentSequenceService documentSequenceService;

    // Uses partCreatedKafkaListenerContainerFactory — deserializes as PartCreatedEvent
    @KafkaListener(
        topics = KafkaTopics.INVENTORY_CREATED,
        containerFactory = "partCreatedKafkaListenerContainerFactory"
    )
    public void consumeCreated(@Payload PartCreatedEvent event) {
        log.info("Inventory record created: {}", event.getName());
    }

    // Uses qaKafkaListenerContainerFactory — deserializes as QAStatusUpdatedEvent
    // Ignores __TypeId__ header completely — maps JSON fields by name
    @KafkaListener(
        topics = KafkaTopics.COMPLIANCE_QA_RESULT,
        containerFactory = "qaKafkaListenerContainerFactory"
    )
    public void consumeQAResult(@Payload QAStatusUpdatedEvent event) {
        log.info("QA Result received — PartId: {}, Batch: {}, Status: {}, Approved: {}, Rejected: {}",
                event.getPartId(), event.getBatchNumber(), event.getStatus(),
                event.getApprovedQty(), event.getRejectedQty());
        try {
            // The external Compliance Service's QA result stands in for a human QA Inspector's
            // sign-off, so it's granted the same authority — this keeps this automated path
            // working unchanged even once a criticality-based release rule is configured.
            partService.processQADecision(
                    event.getPartId(),
                    event.getBatchNumber(),
                    event.getApprovedQty(),
                    event.getRejectedQty(),
                    event.getCriteria(),
                    event.getRemarks() != null && !event.getRemarks().isBlank()
                            ? event.getRemarks() : "Automated update from Compliance Service",
                    java.util.Set.of("QA_INSPECTOR")
            );
            log.info("Stock balance updated for Batch: {}", event.getBatchNumber());
        } catch (Exception e) {
            log.error("Failed to process QA decision — PartId: {}, Batch: {}, Error: {}",
                    event.getPartId(), event.getBatchNumber(), e.getMessage());
        }
    }

    @KafkaListener(
        topics = KafkaTopics.COMPLIANCE_FAI_COMPLETED,
        groupId = "inventory-group"
    )
    public void consumeFaiResult(@Payload FaiResultEvent event) {
        log.info("FAI Result received — FAI: {}, PartId: {}, Result: {}",
                event.getFaiNumber(), event.getPartId(), event.getOverallResult());

        if ("NON_CONFORMING".equalsIgnoreCase(event.getOverallResult())) {
            log.warn("NON_CONFORMING FAI detected for Part {} ({}) — Auto-generating NCR",
                    event.getPartName(), event.getFaiNumber());
            try {
                // Dedup against Kafka at-least-once redelivery: this same FAI-completed event
                // landing twice must not double-create an NCR for the same non-conformance.
                if (event.getFaiNumber() != null && ncrRepository.existsBySourceFaiNumber(event.getFaiNumber())) {
                    log.warn("NCR already auto-created for FAI {} — skipping duplicate delivery", event.getFaiNumber());
                    return;
                }

                NonConformanceReport ncr = NonConformanceReport.builder()
                        .ncrNumber(documentSequenceService.generate("NCR"))
                        .partId(event.getPartId())
                        .partName(event.getPartName() != null ? event.getPartName() : "Part #" + event.getPartId())
                        .batchNumber(event.getBatchNumber() != null ? event.getBatchNumber() : "FAI-BATCH")
                        .quantity(1)
                        .reason("Auto-generated from FAI " + event.getFaiNumber() + " NON_CONFORMING result: " + (event.getRemarks() != null ? event.getRemarks() : "First Article Inspection Failed"))
                        .status(NcrStatus.OPEN)
                        .sourceFaiNumber(event.getFaiNumber())
                        .build();

                NonConformanceReport saved = ncrRepository.save(ncr);
                log.info("Auto-created NCR {} for NON_CONFORMING FAI {}", saved.getNcrNumber(), event.getFaiNumber());
            } catch (Exception e) {
                log.error("Failed to process FAI result for NCR generation: {}", e.getMessage());
            }
        }
    }
}