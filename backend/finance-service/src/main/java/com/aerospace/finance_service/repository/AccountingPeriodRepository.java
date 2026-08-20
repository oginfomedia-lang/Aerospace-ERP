package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.AccountingPeriod;
import com.aerospace.finance_service.entity.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {
    Optional<AccountingPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate d1, LocalDate d2);

    default Optional<AccountingPeriod> findPeriodContaining(LocalDate date) {
        return findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
    }

    List<AccountingPeriod> findByStatus(PeriodStatus status);
    long countByStatus(PeriodStatus status);
}
