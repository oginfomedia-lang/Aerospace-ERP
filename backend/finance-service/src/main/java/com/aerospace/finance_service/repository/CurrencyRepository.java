package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
}
