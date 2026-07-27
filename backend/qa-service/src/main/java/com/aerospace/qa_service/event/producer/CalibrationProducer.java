package com.aerospace.qa_service.event.producer;

import com.aerospace.qa_service.event.CalibrationDueEvent;
import com.aerospace.qa_service.event.QAKafkaTopics;
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
public class CalibrationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCalibrationDue(CalibrationDueEvent event) {
        try {
            Message<CalibrationDueEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, QAKafkaTopics.CALIBRATION_DUE)
                    .setHeader("kafka_messageKey", event.getAssetTag())
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish calibration-due event — assetTag: {}, error: {}",
                            event.getAssetTag(), ex.getMessage());
                } else {
                    log.info("Calibration-due event published — assetTag: {}, overdue: {}",
                            event.getAssetTag(), event.isOverdue());
                }
            });
        } catch (Exception e) {
            log.error("Exception publishing calibration-due event: {}", e.getMessage());
        }
    }
}
