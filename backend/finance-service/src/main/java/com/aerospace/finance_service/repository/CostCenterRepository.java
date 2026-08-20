package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {
    Optional<CostCenter> findByCostCenterCode(String costCenterCode);
    List<CostCenter> findByActiveStatusTrue();
    boolean existsByCostCenterCode(String costCenterCode);
}
