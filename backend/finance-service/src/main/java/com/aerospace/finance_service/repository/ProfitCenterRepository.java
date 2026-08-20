package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.ProfitCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfitCenterRepository extends JpaRepository<ProfitCenter, Long> {
    Optional<ProfitCenter> findByProfitCenterCode(String profitCenterCode);
    List<ProfitCenter> findByActiveStatusTrue();
    boolean existsByProfitCenterCode(String profitCenterCode);
}
