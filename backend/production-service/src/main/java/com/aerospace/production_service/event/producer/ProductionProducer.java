package com.aerospace.production_service.event.producer;

import com.aerospace.production_service.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProductionOrderCreated(ProductionOrderCreatedEvent event) {
        log.info("[KAFKA] Publishing PRODUCTION_ORDER_CREATED → poNumber={}", event.getPoNumber());
        kafkaTemplate.send(KafkaTopics.PRODUCTION_ORDER_CREATED,
                String.valueOf(event.getProductionOrderId()), event);
    }

    public void sendProductionOrderStarted(Long orderId, String poNumber, String productName) {
        var payload = java.util.Map.of(
            "productionOrderId", orderId,
            "poNumber",          poNumber,
            "productName",       productName,
            "status",            "IN_PRODUCTION"
        );
        log.info("[KAFKA] Publishing PRODUCTION_ORDER_STARTED → poNumber={}", poNumber);
        kafkaTemplate.send(KafkaTopics.PRODUCTION_ORDER_STARTED, String.valueOf(orderId), payload);
    }

    public void sendProductionOrderCompleted(ProductionOrderCompletedEvent event) {
        log.info("[KAFKA] Publishing PRODUCTION_ORDER_COMPLETED → poNumber={}", event.getPoNumber());
        kafkaTemplate.send(KafkaTopics.PRODUCTION_ORDER_COMPLETED,
                String.valueOf(event.getProductionOrderId()), event);
    }

    public void sendProductionOrderOnHold(Long orderId, String poNumber, String reason) {
        var payload = java.util.Map.of(
            "productionOrderId", orderId,
            "poNumber",          poNumber,
            "reason",            reason != null ? reason : "",
            "status",            "ON_HOLD"
        );
        log.info("[KAFKA] Publishing PRODUCTION_ORDER_ON_HOLD → poNumber={}", poNumber);
        kafkaTemplate.send(KafkaTopics.PRODUCTION_ORDER_ON_HOLD, String.valueOf(orderId), payload);
    }

    public void sendProductionOrderCancelled(Long orderId, String poNumber, String reason) {
        var payload = java.util.Map.of(
            "productionOrderId", orderId,
            "poNumber",          poNumber,
            "reason",            reason != null ? reason : "",
            "status",            "CANCELLED"
        );
        log.info("[KAFKA] Publishing PRODUCTION_ORDER_CANCELLED → poNumber={}", poNumber);
        kafkaTemplate.send(KafkaTopics.PRODUCTION_ORDER_CANCELLED, String.valueOf(orderId), payload);
    }

    public void sendStageLogAdded(Long orderId, String poNumber, String stageName, String stageStatus) {
        var payload = java.util.Map.of(
            "productionOrderId", orderId,
            "poNumber",          poNumber,
            "stageName",         stageName,
            "stageStatus",       stageStatus
        );
        log.info("[KAFKA] Publishing STAGE_LOG_ADDED → poNumber={} stage={}", poNumber, stageName);
        kafkaTemplate.send(KafkaTopics.STAGE_LOG_ADDED, String.valueOf(orderId), payload);
    }
}
