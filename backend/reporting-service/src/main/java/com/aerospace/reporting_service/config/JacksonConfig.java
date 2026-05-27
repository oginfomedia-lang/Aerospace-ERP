package com.aerospace.reporting_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit configuration for Jackson's {@link ObjectMapper}.
 *
 * Although Spring Boot's {@code spring-boot-starter-json} normally provides an
 * {@code ObjectMapper} bean automatically, the application was failing to
 * autowire it in the Kafka consumers. Defining the bean explicitly guarantees
 * that the bean exists regardless of any auto‑configuration conditions or
 * dependency exclusions.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
