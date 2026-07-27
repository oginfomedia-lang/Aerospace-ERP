package com.aerospace.qa_service.event.producer;

import com.aerospace.qa_service.event.QAKafkaTopics;
import com.aerospace.qa_service.event.QAResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class QAProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public boolean publishQAResult(QAResultEvent event) {
        try {
            // FIX: Build message manually WITHOUT __TypeId__ header
            // Inventory consumer uses @Payload with explicit type — no header needed
            // JSON fields map directly: partId, batchNumber, status, location
            Message<QAResultEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, QAKafkaTopics.COMPLIANCE_QA_RESULT)
                    .setHeader("kafka_messageKey", String.valueOf(event.getPartId()))
                    .build();

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(message);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish QA result — partId: {}, batch: {}, error: {}",
                            event.getPartId(), event.getBatchNumber(), ex.getMessage());
                } else {
                    log.info("QA result published — partId: {}, batch: {}, decision: {}, offset: {}",
                            event.getPartId(), event.getBatchNumber(), event.getStatus(),
                            result.getRecordMetadata().offset());
                }
            });

            return true;
        } catch (Exception e) {
            log.error("Exception publishing QA result: {}", e.getMessage());
            return false;
        }
    }

    public boolean publishFaiResult(com.aerospace.qa_service.event.FaiResultEvent event) {
        try {
            Message<com.aerospace.qa_service.event.FaiResultEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, QAKafkaTopics.COMPLIANCE_FAI_COMPLETED)
                    .setHeader("kafka_messageKey", String.valueOf(event.getPartId()))
                    .build();

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish FAI result — faiNumber: {}, partId: {}, error: {}",
                            event.getFaiNumber(), event.getPartId(), ex.getMessage());
                } else {
                    log.info("FAI result published — faiNumber: {}, partId: {}, result: {}, offset: {}",
                            event.getFaiNumber(), event.getPartId(), event.getOverallResult(),
                            result.getRecordMetadata().offset());
                }
            });

            return true;
        } catch (Exception e) {
            log.error("Exception publishing FAI result: {}", e.getMessage());
            return false;
        }
    }
}