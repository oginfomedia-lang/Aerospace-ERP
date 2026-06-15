package com.aerospace.api_gateway.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Add custom health checks here (e.g., check if all services are reachable)
        return Health.up()
                .withDetail("service", "API Gateway")
                .withDetail("status", "running")
                .build();
    }
}