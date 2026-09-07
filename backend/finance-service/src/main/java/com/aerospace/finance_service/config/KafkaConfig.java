package com.aerospace.finance_service.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

/**
 * finance-service has no custom producer/consumer Kafka beans of its own — its
 * @KafkaListener consumers run on Spring Boot's auto-configured
 * ConcurrentKafkaListenerContainerFactory, and this DefaultErrorHandler is the
 * single CommonErrorHandler bean that autoconfiguration wires into it.
 *
 * Retries a failing message 3 times (2s backoff) then publishes it to
 * "<original-topic>.DLT" instead of logging and dropping it.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3));
        // Don't retry-then-DLT things that will never succeed no matter how many times you
        // retry them (bad JSON, wrong type) — send those straight to the DLT instead of
        // wasting 3 retries on something deterministic.
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }
}
