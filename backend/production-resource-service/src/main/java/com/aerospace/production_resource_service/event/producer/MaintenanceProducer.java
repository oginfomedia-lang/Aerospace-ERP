package com.aerospace.production_resource_service.event.producer;

import com.aerospace.production_resource_service.event.MaintenanceDueEvent;
import com.aerospace.production_resource_service.event.ProductionResourceKafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishMaintenanceDue(MaintenanceDueEvent event) {
        try {
            Message<MaintenanceDueEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, ProductionResourceKafkaTopics.MAINTENANCE_DUE)
                    .setHeader("kafka_messageKey", event.getMachineCode())
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish maintenance-due event — machineCode: {}, error: {}",
                            event.getMachineCode(), ex.getMessage());
                } else {
                    log.info("Maintenance-due event published — machineCode: {}, overdue: {}",
                            event.getMachineCode(), event.isOverdue());
                }
            });
        } catch (Exception e) {
            log.error("Exception publishing maintenance-due event: {}", e.getMessage());
        }
    }
}
