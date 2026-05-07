package com.aerospace.order_service.event.producer;

import com.aerospace.order_service.event.KafkaTopics;
import com.aerospace.order_service.event.WorkOrderEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkOrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCreated(WorkOrderCreatedEvent event) {
        log.info("[KAFKA] Publishing WORK_ORDER_CREATED → woNumber={}", event.getWoNumber());
        kafkaTemplate.send(KafkaTopics.WORK_ORDER_CREATED,
                String.valueOf(event.getWorkOrderId()), event);
    }

    public void publishStatusChanged(WorkOrderStatusChangedEvent event) {
        log.info("[KAFKA] Publishing WORK_ORDER_UPDATED → woNumber={} {} → {}",
                event.getWoNumber(), event.getFromStatus(), event.getToStatus());
        kafkaTemplate.send(KafkaTopics.WORK_ORDER_UPDATED,
                String.valueOf(event.getWorkOrderId()), event);
    }

    public void publishCompleted(WorkOrderCompletedEvent event) {
        log.info("[KAFKA] Publishing WORK_ORDER_COMPLETED → woNumber={}", event.getWoNumber());
        kafkaTemplate.send(KafkaTopics.WORK_ORDER_COMPLETED,
                String.valueOf(event.getWorkOrderId()), event);
    }

    public void publishMaterialRequest(MaterialRequestEvent event) {
        log.info("[KAFKA] Publishing MATERIAL_REQUEST → woNumber={} partId={}",
                event.getWoNumber(), event.getPartId());
        kafkaTemplate.send(KafkaTopics.MATERIAL_REQUEST,
                String.valueOf(event.getWorkOrderId()), event);
    }
}