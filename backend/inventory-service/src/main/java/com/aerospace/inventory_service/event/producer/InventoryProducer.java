package com.aerospace.inventory_service.event.producer;

import com.aerospace.inventory_service.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── PART CREATED ─────────────────────────────────────────────────────────
    // Topic: part-created-events
    // Also publishes to inventory.created for internal consumers (InventoryConsumer)

    public void sendCreatedEvent(PartCreatedEvent event) {
        log.info("[KAFKA] Publishing PART_CREATED for partId={}", event.getPartId());
        kafkaTemplate.send(KafkaTopics.PART_CREATED, String.valueOf(event.getPartId()), event);
        kafkaTemplate.send(KafkaTopics.INVENTORY_CREATED, String.valueOf(event.getPartId()), event);
    }

    // ── STOCK RECEIVED ────────────────────────────────────────────────────────
    // Topic: stock-received-events

    public void sendStockReceivedEvent(StockReceivedEvent event) {
        log.info("[KAFKA] Publishing STOCK_RECEIVED for partId={}", event.getPartId());
        kafkaTemplate.send(KafkaTopics.STOCK_RECEIVED, String.valueOf(event.getPartId()), event);
    }

    // ── STOCK LOW ─────────────────────────────────────────────────────────────
    // Topic: stock-low-events

    public void sendLowStockEvent(StockLowEvent event) {
        log.info("[KAFKA] Publishing STOCK_LOW for partId={}", event.getPartId());
        kafkaTemplate.send(KafkaTopics.STOCK_LOW, String.valueOf(event.getPartId()), event);
    }

    // ── MATERIAL ISSUED ───────────────────────────────────────────────────────
    // Topic: material-issued-events

    public void sendMaterialIssuedEvent(MaterialIssuedEvent event) {
        log.info("[KAFKA] Publishing MATERIAL_ISSUED for partId={}", event.getPartId());
        kafkaTemplate.send(KafkaTopics.MATERIAL_ISSUED, String.valueOf(event.getPartId()), event);
    }

    // ── QA STATUS CHANGED ─────────────────────────────────────────────────────
    // Topic: qa-status-changed-events

    public void sendQAStatusChangedEvent(QAStatusChangedEvent event) {
        log.info("[KAFKA] Publishing QA_STATUS_CHANGED for partId={} status={}",
                event.getPartId(), event.getNewStatus());
        kafkaTemplate.send(KafkaTopics.QA_STATUS_CHANGED, String.valueOf(event.getPartId()), event);
    }

    // Convenience overload — builds minimal event from raw fields
    public void sendQAStatusChangedEvent(Long partId, String batchNumber, String status) {
        sendQAStatusChangedEvent(QAStatusChangedEvent.builder()
                .partId(partId)
                .batchNumber(batchNumber)
                .newStatus(status)
                .decision(status)
                .build());
    }
}