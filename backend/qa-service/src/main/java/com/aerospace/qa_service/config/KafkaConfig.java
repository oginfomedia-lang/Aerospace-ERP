package com.aerospace.qa_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    // Auto-creates the topic if it doesn't exist in Kafka yet
    // partitions=1 for single broker dev setup
    // replication=1 since we only have one Kafka broker in Docker
    @Bean
    public NewTopic qaResultTopic() {
        return TopicBuilder.name("compliance.qa.result")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic calibrationDueTopic() {
        return TopicBuilder.name("qa.calibration.due")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // Wired into the auto-configured ConcurrentKafkaListenerContainerFactory by
    // Spring Boot's Kafka autoconfiguration (single CommonErrorHandler bean present).
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3));
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }
}