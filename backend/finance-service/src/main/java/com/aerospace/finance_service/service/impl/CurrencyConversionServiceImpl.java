package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.entity.ExchangeRate;
import com.aerospace.finance_service.repository.ExchangeRateRepository;
import com.aerospace.finance_service.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency, LocalDate asOf) {
        if (fromCurrency.equals(toCurrency)) return amount;
        return amount.multiply(getRate(fromCurrency, toCurrency, asOf)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getRate(String fromCurrency, String toCurrency, LocalDate asOf) {
        return exchangeRateRepository
                .findFirstByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(fromCurrency, toCurrency, asOf)
                .map(ExchangeRate::getRate)
                .orElseThrow(() -> new IllegalStateException("No exchange rate found for " + fromCurrency + "->" + toCurrency + " as of " + asOf));
    }
}
