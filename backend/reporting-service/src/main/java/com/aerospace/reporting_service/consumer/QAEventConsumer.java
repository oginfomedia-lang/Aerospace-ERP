package com.aerospace.reporting_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aerospace.reporting_service.entity.QAFact;
import com.aerospace.reporting_service.event.QAStatusChangedEvent;
import com.aerospace.reporting_service.repository.QAFactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class QAEventConsumer {

    private final QAFactRepository qaFactRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "qa-status-changed-events", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handleQAStatusChanged(String message) {
        try {
            QAStatusChangedEvent event = objectMapper.readValue(message, QAStatusChangedEvent.class);
            log.info("📋 QA decision recorded: {} – {}", event.getBatchNumber(), event.getDecision());
            QAFact fact = new QAFact();
            fact.setPartId(event.getPartId());
            fact.setPartName(event.getPartName());
            fact.setBatchNumber(event.getBatchNumber());
            fact.setDecision(event.getNewStatus());   // APPROVED or REJECTED
            fact.setInspectorName(event.getInspectorName());
            fact.setInspectedAt(LocalDateTime.now());
            qaFactRepository.save(fact);
        } catch (Exception e) {
            log.error("Failed to parse QA status message: {}", message, e);
        }
    }
}