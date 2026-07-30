package com.aerospace.production_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.aerospace.production_service", "com.aerospace.common"})
@EntityScan(basePackages = {"com.aerospace.production_service.entity", "com.aerospace.common"})
@EnableJpaRepositories(basePackages = {"com.aerospace.production_service.repository", "com.aerospace.common"})
@EnableDiscoveryClient
public class ProductionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductionServiceApplication.class, args);
	}

}
