package com.aerospace.auth_service.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

// auth-service's first outbound event (SupplierInviteCreatedEvent) — without this, Spring
// Boot's autoconfigured KafkaTemplate defaults to StringSerializer for values, which can't
// serialize a POJO. Mirrors procurement-service's KafkaConfig producer bean exactly.
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // Same SASL/PLAIN config every other service's application.yml now carries under
    // spring.kafka.properties — this service configures its producer in Java instead of
    // YAML (see class comment above), so it needs the equivalent set here by hand rather
    // than picking it up automatically. security.protocol defaults to PLAINTEXT (today's
    // real local-dev behavior, unchanged); the VPS sets KAFKA_SECURITY_PROTOCOL=SASL_PLAINTEXT.
    @Value("${KAFKA_SECURITY_PROTOCOL:PLAINTEXT}")
    private String securityProtocol;

    @Value("${KAFKA_USERNAME:}")
    private String kafkaUsername;

    @Value("${KAFKA_PASSWORD:}")
    private String kafkaPassword;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put("security.protocol", securityProtocol);
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                + kafkaUsername + "\" password=\"" + kafkaPassword + "\";");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
