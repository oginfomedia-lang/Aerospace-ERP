package com.aerospace.production_service.event.consumer;

import com.aerospace.production_service.event.KafkaTopics;
import com.aerospace.production_service.event.MaterialIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductionConsumer {

    // Listens to inventory-service MATERIAL_ISSUED events.
    // Use this to track which parts have been drawn for a production order.
    @KafkaListener(
        topics = KafkaTopics.MATERIAL_ISSUED,
        containerFactory = "materialIssuedKafkaListenerContainerFactory"
    )
    public void consumeMaterialIssued(@Payload MaterialIssuedEvent event) {
        log.info("[KAFKA] MATERIAL_ISSUED received — partId={}, batch={}, qty={}, issuedTo={}",
                event.getPartId(), event.getBatchNumber(),
                event.getQuantityIssued(), event.getIssuedTo());

        // Future: auto-match event to an open production order and update
        // material consumption status from PENDING → CONSUMED.
        // For now, we log for full Kafka integration traceability.
    }

    // Listens to inventory-service STOCK_LOW events.
    // Use this to put related production orders ON_HOLD automatically.
    @KafkaListener(
        topics = KafkaTopics.STOCK_LOW,
        groupId = "production-stock-alert-group"
    )
    public void consumeStockLow(@Payload String rawEvent) {
        log.warn("[KAFKA] STOCK_LOW alert received — {}", rawEvent);
        // Future: parse event, find production orders using that partId,
        // flag them ON_HOLD and publish ProductionOrderOnHoldEvent.
    }
}
