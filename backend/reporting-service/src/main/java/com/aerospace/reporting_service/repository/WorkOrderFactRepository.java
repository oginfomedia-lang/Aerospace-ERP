// WorkOrderFactRepository.java
package com.aerospace.reporting_service.repository;

import com.aerospace.reporting_service.entity.WorkOrderFact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderFactRepository extends JpaRepository<WorkOrderFact, Long> {
    long countByStatus(String status);
}