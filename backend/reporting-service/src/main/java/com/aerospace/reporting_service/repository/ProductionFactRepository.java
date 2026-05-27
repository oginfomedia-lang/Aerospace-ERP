// ProductionFactRepository.java
package com.aerospace.reporting_service.repository;

import com.aerospace.reporting_service.entity.ProductionFact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionFactRepository extends JpaRepository<ProductionFact, Long> {
}