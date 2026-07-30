package com.aerospace.notification_service.config;

import com.aerospace.notification_service.event.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Shared base consumer properties ────────────────────────────────────

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        return props;
    }

    // ── Generic typed factory builder ───────────────────────────────────────

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> factory(Class<T> clazz) {
        JsonDeserializer<T> deser = new JsonDeserializer<>(clazz);
        deser.setUseTypeHeaders(false);
        deser.addTrustedPackages("*");

        ErrorHandlingDeserializer<T> errorDeser = new ErrorHandlingDeserializer<>(deser);

        ConsumerFactory<String, T> cf = new DefaultKafkaConsumerFactory<>(
                baseProps(), new StringDeserializer(), errorDeser);

        ConcurrentKafkaListenerContainerFactory<String, T> f =
                new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(cf);

        // Retry failed messages 3 times with 2s backoff before logging and skipping
        f.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(2000L, 3)));

        return f;
    }

    // ── Inventory event factories ─────────────────────────────────────────────

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockLowEvent>
    stockLowListenerFactory() {
        return factory(StockLowEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QAStatusChangedEvent>
    qaStatusListenerFactory() {
        return factory(QAStatusChangedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MaterialIssuedEvent>
    materialIssuedListenerFactory() {
        return factory(MaterialIssuedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockReceivedEvent>
    stockReceivedListenerFactory() {
        return factory(StockReceivedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PartCreatedEvent>
    partCreatedListenerFactory() {
        return factory(PartCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VendorReturnEvent>
    vendorReturnListenerFactory() {
        return factory(VendorReturnEvent.class);
    }

    // ── Work order event factories ────────────────────────────────────────────

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkOrderCreatedEvent>
    workOrderCreatedListenerFactory() {
        return factory(WorkOrderCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkOrderStatusChangedEvent>
    workOrderUpdatedListenerFactory() {
        return factory(WorkOrderStatusChangedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkOrderCompletedEvent>
    workOrderCompletedListenerFactory() {
        return factory(WorkOrderCompletedEvent.class);
    }

     @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RfqCreatedEvent>
    rfqCreatedListenerFactory() {
        return factory(RfqCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QuotationAwardedEvent>
    quotationAwardedListenerFactory() {
        return factory(QuotationAwardedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QuotationRejectedEvent>
    quotationRejectedListenerFactory() {
        return factory(QuotationRejectedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SupplierInviteCreatedEvent>
    supplierInviteListenerFactory() {
        return factory(SupplierInviteCreatedEvent.class);
    }

    // ── QA event factories ──────────────────────────────────────────────────

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CalibrationDueEvent>
    calibrationDueListenerFactory() {
        return factory(CalibrationDueEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FaiResultEvent>
    faiResultListenerFactory() {
        return factory(FaiResultEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CapaEvent>
    capaEventListenerFactory() {
        return factory(CapaEvent.class);
    }

    @Bean
    public ConsumerFactory<String, ApprovalEscalatedEvent> approvalEscalatedConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        JsonDeserializer<ApprovalEscalatedEvent> deserializer = new JsonDeserializer<>(ApprovalEscalatedEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new ErrorHandlingDeserializer<>(deserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ApprovalEscalatedEvent> approvalEscalatedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ApprovalEscalatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(approvalEscalatedConsumerFactory());
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
                new org.springframework.util.backoff.FixedBackOff(2000L, 3)));
        return factory;
    }

}