package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    // "latest rate as of this date" — the standard accounting lookup
    Optional<ExchangeRate> findFirstByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            String fromCurrency, String toCurrency, LocalDate asOf);
}
