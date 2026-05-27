// InventoryFactRepository.java
package com.aerospace.reporting_service.repository;

import com.aerospace.reporting_service.entity.InventoryFact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InventoryFactRepository extends JpaRepository<InventoryFact, Long> {
    Optional<InventoryFact> findByBatchNumber(String batchNumber);
    long countByStatus(String status);
}