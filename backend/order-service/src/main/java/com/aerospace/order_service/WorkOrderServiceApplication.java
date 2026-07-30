package com.aerospace.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.aerospace.order_service", "com.aerospace.common"})
@EntityScan(basePackages = {"com.aerospace.order_service.entity", "com.aerospace.common"})
@EnableJpaRepositories(basePackages = {"com.aerospace.order_service.repository", "com.aerospace.common"})
@EnableDiscoveryClient
public class WorkOrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkOrderServiceApplication.class, args);
    }
}