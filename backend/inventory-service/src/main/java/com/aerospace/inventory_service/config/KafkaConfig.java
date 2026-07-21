package com.aerospace.inventory_service.config;

import com.aerospace.inventory_service.event.PartCreatedEvent;
import com.aerospace.inventory_service.event.ProductionOrderCompletedEvent;
import com.aerospace.inventory_service.event.QAStatusUpdatedEvent;
import com.aerospace.inventory_service.event.WorkOrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;


import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Shared base consumer properties
    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        return props;
    }

    // Factory for QAStatusUpdatedEvent — used by compliance.qa.result topic
    @Bean
    public ConsumerFactory<String, QAStatusUpdatedEvent> qaConsumerFactory() {
        Map<String, Object> props = baseConsumerProps();

        JsonDeserializer<QAStatusUpdatedEvent> deserializer =
                new JsonDeserializer<>(QAStatusUpdatedEvent.class);
        deserializer.setUseTypeHeaders(false); // ignore __TypeId__ header
        deserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<QAStatusUpdatedEvent> errorHandling =
                new ErrorHandlingDeserializer<>(deserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QAStatusUpdatedEvent> qaKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, QAStatusUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(qaConsumerFactory());
        return factory;
    }

    // Factory for PartCreatedEvent — used by inventory.created topic
    @Bean
    public ConsumerFactory<String, PartCreatedEvent> partCreatedConsumerFactory() {
        Map<String, Object> props = baseConsumerProps();

        JsonDeserializer<PartCreatedEvent> deserializer =
                new JsonDeserializer<>(PartCreatedEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<PartCreatedEvent> errorHandling =
                new ErrorHandlingDeserializer<>(deserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PartCreatedEvent> partCreatedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PartCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(partCreatedConsumerFactory());
        return factory;
    }

    // Factory for WorkOrderCreatedEvent — used by work-order-created-events topic
    @Bean
    public ConsumerFactory<String, WorkOrderCreatedEvent> workOrderCreatedConsumerFactory() {
        Map<String, Object> props = baseConsumerProps();

        JsonDeserializer<WorkOrderCreatedEvent> deserializer =
                new JsonDeserializer<>(WorkOrderCreatedEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<WorkOrderCreatedEvent> errorHandling =
                new ErrorHandlingDeserializer<>(deserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkOrderCreatedEvent> workOrderCreatedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WorkOrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(workOrderCreatedConsumerFactory());
        return factory;
    }

    @Bean
        public ConsumerFactory<String, ProductionOrderCompletedEvent> productionOrderCompletedConsumerFactory() {
        Map<String, Object> props = baseConsumerProps();
        JsonDeserializer<ProductionOrderCompletedEvent> deserializer =
                new JsonDeserializer<>(ProductionOrderCompletedEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        ErrorHandlingDeserializer<ProductionOrderCompletedEvent> errorHandling =
                new ErrorHandlingDeserializer<>(deserializer);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandling);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, ProductionOrderCompletedEvent> productionOrderCompletedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ProductionOrderCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(productionOrderCompletedConsumerFactory());
        return factory;
        }

}