package com.aerospace.finance_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CurrencyConversionService {
    BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency, LocalDate asOf);
    BigDecimal getRate(String fromCurrency, String toCurrency, LocalDate asOf);
}
