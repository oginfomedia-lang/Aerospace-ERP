package com.aerospace.reporting_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aerospace.reporting_service.entity.InventoryFact;
import com.aerospace.reporting_service.event.StockReceivedEvent;
import com.aerospace.reporting_service.event.QAStatusChangedEvent;
import com.aerospace.reporting_service.event.MaterialIssuedEvent;
import com.aerospace.reporting_service.repository.InventoryFactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final InventoryFactRepository inventoryFactRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "stock-received-events", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handleStockReceived(String message) {
        try {
            StockReceivedEvent event = objectMapper.readValue(message, StockReceivedEvent.class);
            log.info("📦 Stock received: Part {} batch {}", event.getPartName(), event.getBatchNumber());
            InventoryFact fact = InventoryFact.builder()
                    .partId(event.getPartId())
                    .partName(event.getPartName())
                    .batchNumber(event.getBatchNumber())
                    .quantity(event.getQuantityReceived())
                    .status("PENDING_QA")
                    .location(event.getLocation())
                    .lastUpdated(LocalDateTime.now())
                    .build();
            inventoryFactRepository.save(fact);
        } catch (Exception e) {
            log.error("Failed to parse stock-received message: {}", message, e);
        }
    }

    @KafkaListener(topics = "qa-status-changed-events", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handleQAStatusChanged(String message) {
        try {
            QAStatusChangedEvent event = objectMapper.readValue(message, QAStatusChangedEvent.class);
            log.info("🔍 QA status changed for batch {}: {}", event.getBatchNumber(), event.getNewStatus());
            inventoryFactRepository.findByBatchNumber(event.getBatchNumber()).ifPresent(fact -> {
                fact.setStatus(event.getNewStatus());
                fact.setLastUpdated(LocalDateTime.now());
                inventoryFactRepository.save(fact);
            });
        } catch (Exception e) {
            log.error("Failed to parse QA status message: {}", message, e);
        }
    }

    @KafkaListener(topics = "material-issued-events", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handleMaterialIssued(String message) {
        try {
            MaterialIssuedEvent event = objectMapper.readValue(message, MaterialIssuedEvent.class);
            log.info("🔧 Material issued: {} x {}", event.getPartName(), event.getQuantityIssued());
            inventoryFactRepository.findByBatchNumber(event.getBatchNumber()).ifPresent(fact -> {
                fact.setQuantity(fact.getQuantity() - event.getQuantityIssued());
                fact.setLastUpdated(LocalDateTime.now());
                inventoryFactRepository.save(fact);
            });
        } catch (Exception e) {
            log.error("Failed to parse material-issued message: {}", message, e);
        }
    }
}