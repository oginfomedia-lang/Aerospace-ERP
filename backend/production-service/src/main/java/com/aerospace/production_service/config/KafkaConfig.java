package com.aerospace.production_service.config;

import com.aerospace.production_service.event.MaterialIssuedEvent;
import com.aerospace.production_service.event.WorkOrderStatusChangedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaConfig(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3));
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }

    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "production-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        return props;
    }

    // Listens to material-issued-events from inventory-service
    @Bean
    public ConsumerFactory<String, MaterialIssuedEvent> materialIssuedConsumerFactory() {
        JsonDeserializer<MaterialIssuedEvent> deserializer =
                new JsonDeserializer<>(MaterialIssuedEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        ErrorHandlingDeserializer<MaterialIssuedEvent> errorHandling =
                new ErrorHandlingDeserializer<>(deserializer);
        return new DefaultKafkaConsumerFactory<>(baseConsumerProps(),
                new StringDeserializer(), errorHandling);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MaterialIssuedEvent>
            materialIssuedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MaterialIssuedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(materialIssuedConsumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // Listens to work-order-updated-events from order-service — the OPEN→IN_PROGRESS
    // transition carries a materialLineItems snapshot used to auto-create a
    // ProductionOrder + BOM (see WorkOrderConsumer).
    @Bean
    public ConsumerFactory<String, WorkOrderStatusChangedEvent> workOrderStatusChangedConsumerFactory() {
        JsonDeserializer<WorkOrderStatusChangedEvent> deserializer =
                new JsonDeserializer<>(WorkOrderStatusChangedEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        ErrorHandlingDeserializer<WorkOrderStatusChangedEvent> errorHandling =
                new ErrorHandlingDeserializer<>(deserializer);
        return new DefaultKafkaConsumerFactory<>(baseConsumerProps(),
                new StringDeserializer(), errorHandling);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkOrderStatusChangedEvent>
            workOrderStatusChangedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WorkOrderStatusChangedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(workOrderStatusChangedConsumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }
}
