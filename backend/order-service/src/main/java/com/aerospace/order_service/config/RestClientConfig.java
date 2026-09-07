package com.aerospace.order_service.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {
    // A bare `new RestTemplate()` has no timeout at all (connect/read default to -1,
    // meaning infinite) — a hung downstream service would block this caller's thread
    // forever. 5s to connect / 10s to read is generous for an internal service call but
    // bounds the failure instead of leaving it open-ended.
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}
