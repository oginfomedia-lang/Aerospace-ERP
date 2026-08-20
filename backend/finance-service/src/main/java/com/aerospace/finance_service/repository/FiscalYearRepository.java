package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {
    Optional<FiscalYear> findByFiscalYearCode(String fiscalYearCode);
    boolean existsByFiscalYearCode(String fiscalYearCode);
}
