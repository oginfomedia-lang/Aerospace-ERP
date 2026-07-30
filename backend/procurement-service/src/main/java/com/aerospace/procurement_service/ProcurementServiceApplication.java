package com.aerospace.procurement_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.aerospace.procurement_service", "com.aerospace.common"})
@EntityScan(basePackages = {"com.aerospace.procurement_service.entity", "com.aerospace.common"})
@EnableJpaRepositories(basePackages = {"com.aerospace.procurement_service.repository", "com.aerospace.common"})
@EnableDiscoveryClient
@EnableScheduling
public class ProcurementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcurementServiceApplication.class, args);
    }
}