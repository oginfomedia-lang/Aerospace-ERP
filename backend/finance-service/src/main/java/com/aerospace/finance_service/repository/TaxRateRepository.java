package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    Optional<TaxRate> findByTaxRuleCodeAndActiveStatusTrue(String taxRuleCode);
}
