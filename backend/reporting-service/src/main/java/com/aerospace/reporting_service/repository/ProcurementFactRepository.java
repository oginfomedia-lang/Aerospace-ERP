// ProcurementFactRepository.java
package com.aerospace.reporting_service.repository;

import com.aerospace.reporting_service.entity.ProcurementFact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementFactRepository extends JpaRepository<ProcurementFact, Long> {
}