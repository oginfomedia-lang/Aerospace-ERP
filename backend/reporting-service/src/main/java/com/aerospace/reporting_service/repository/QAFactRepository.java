// QAFactRepository.java
package com.aerospace.reporting_service.repository;

import com.aerospace.reporting_service.entity.QAFact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QAFactRepository extends JpaRepository<QAFact, Long> {
    long countByDecision(String decision);
}