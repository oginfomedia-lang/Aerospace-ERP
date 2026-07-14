package com.aerospace.procurement_service.config;

import com.aerospace.procurement_service.event.MaterialShortageEvent;
import com.aerospace.procurement_service.event.QAStatusChangedEvent;
import com.aerospace.procurement_service.event.StockLowEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ==========================================
    // 1. PRODUCER CONFIGURATION
    // ==========================================
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Serialize keys as normal Strings
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Serialize values (like PurchaseOrderCreatedEvent) as JSON
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Ensure high reliability for Aerospace ERP
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ==========================================
    // 2. CONSUMER CONFIGURATION
    // ==========================================

    @Bean
    public ConsumerFactory<String, StockLowEvent> stockLowConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "procurement-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        // The Key is a normal string
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // The Value is handled by the ErrorHandlingDeserializer
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // Configure the JSON Deserializer explicitly
        JsonDeserializer<StockLowEvent> deserializer = new JsonDeserializer<>(StockLowEvent.class);
        
        // CRITICAL FOR MICROSERVICES: Ignore the class type header sent by inventory-service, 
        // just map the JSON directly to our local StockLowEvent class.
        deserializer.setUseTypeHeaders(false); 
        deserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<StockLowEvent> errorHandlingDeserializer = 
                new ErrorHandlingDeserializer<>(deserializer);

        return new DefaultKafkaConsumerFactory<>(
                props, 
                new StringDeserializer(), 
                errorHandlingDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockLowEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, StockLowEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stockLowConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, MaterialShortageEvent> materialShortageConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "procurement-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        JsonDeserializer<MaterialShortageEvent> deserializer = new JsonDeserializer<>(MaterialShortageEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<MaterialShortageEvent> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(deserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MaterialShortageEvent> materialShortageKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MaterialShortageEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(materialShortageConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, QAStatusChangedEvent> qaStatusChangedConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "procurement-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        JsonDeserializer<QAStatusChangedEvent> deserializer = new JsonDeserializer<>(QAStatusChangedEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<QAStatusChangedEvent> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(deserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QAStatusChangedEvent> qaStatusChangedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, QAStatusChangedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(qaStatusChangedConsumerFactory());
        return factory;
    }
}
