package com.aerospace.finance_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

// scanBasePackages/@EntityScan/@EnableJpaRepositories BOTH list our own package AND
// com.aerospace.common — that second half is what makes common-lib's shared tables
// (audit_log, approval_matrix, approval_history, document_sequence) auto-create in
// finance_db, and makes DocumentSequenceService/ApprovalEngine/AuditAspect injectable
// here exactly as if they were our own code. Copied from SalesServiceApplication.
@SpringBootApplication(scanBasePackages = {"com.aerospace.finance_service", "com.aerospace.common"})
@EntityScan(basePackages = {"com.aerospace.finance_service.entity", "com.aerospace.common"})
@EnableJpaRepositories(basePackages = {"com.aerospace.finance_service.repository", "com.aerospace.common"})
@EnableDiscoveryClient
@EnableScheduling // required for common-lib's ApprovalEscalationScheduler (hourly sweep)
public class FinanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}
