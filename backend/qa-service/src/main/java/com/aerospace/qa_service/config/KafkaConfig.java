package com.aerospace.qa_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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
}