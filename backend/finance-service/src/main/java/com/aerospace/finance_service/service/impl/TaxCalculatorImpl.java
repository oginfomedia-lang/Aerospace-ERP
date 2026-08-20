package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.TaxCalculationResult;
import com.aerospace.finance_service.entity.TaxRate;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.TaxRateRepository;
import com.aerospace.finance_service.service.TaxCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class TaxCalculatorImpl implements TaxCalculator {

    private final TaxRateRepository taxRateRepository;

    @Override
    public TaxCalculationResult calculate(String taxRuleCode, BigDecimal lineAmount) {
        TaxRate rate = taxRateRepository.findByTaxRuleCodeAndActiveStatusTrue(taxRuleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tax rule not found or inactive: " + taxRuleCode));

        BigDecimal taxAmount = lineAmount.multiply(rate.getRatePercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (!rate.getReverseCharge()) {
            return TaxCalculationResult.builder()
                    .taxAmount(taxAmount).taxAccountCode(rate.getTaxAccountCode())
                    .direction(rate.getDirection()).reverseCharge(false).build();
        }
        // reverse charge: PostingEngine adds BOTH lines instead of one
        return TaxCalculationResult.builder()
                .taxAmount(taxAmount).taxAccountCode(rate.getTaxAccountCode())
                .direction(rate.getDirection()).reverseCharge(true)
                .reverseChargeCounterAccountCode(rate.getTaxAccountCode() + "_RC_OFFSET")
                .build();
    }
}
