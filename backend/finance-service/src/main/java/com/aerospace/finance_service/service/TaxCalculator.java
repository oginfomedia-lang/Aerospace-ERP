package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.TaxCalculationResult;

import java.math.BigDecimal;

public interface TaxCalculator {
    TaxCalculationResult calculate(String taxRuleCode, BigDecimal lineAmount);
}
