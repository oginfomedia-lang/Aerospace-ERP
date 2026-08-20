package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.CurrencyDTO;
import com.aerospace.finance_service.dto.ExchangeRateDTO;
import com.aerospace.finance_service.entity.Currency;
import com.aerospace.finance_service.entity.ExchangeRate;
import com.aerospace.finance_service.repository.CurrencyRepository;
import com.aerospace.finance_service.repository.ExchangeRateRepository;
import com.aerospace.finance_service.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    @Transactional
    public CurrencyDTO createCurrency(CurrencyDTO dto) {
        Currency entity = Currency.builder()
                .currencyCode(dto.getCurrencyCode().toUpperCase())
                .currencyName(dto.getCurrencyName())
                .isBaseCurrency(dto.getIsBaseCurrency() != null && dto.getIsBaseCurrency())
                .activeStatus(dto.getActiveStatus() == null || dto.getActiveStatus())
                .build();
        return toDTO(currencyRepository.save(entity));
    }

    @Override
    public List<CurrencyDTO> getAllCurrencies() {
        return currencyRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public ExchangeRateDTO createExchangeRate(ExchangeRateDTO dto) {
        ExchangeRate entity = ExchangeRate.builder()
                .fromCurrency(dto.getFromCurrency().toUpperCase())
                .toCurrency(dto.getToCurrency().toUpperCase())
                .rate(dto.getRate())
                .effectiveDate(dto.getEffectiveDate())
                .build();
        return toDTO(exchangeRateRepository.save(entity));
    }

    @Override
    public List<ExchangeRateDTO> getAllExchangeRates() {
        return exchangeRateRepository.findAll().stream().map(this::toDTO).toList();
    }

    private CurrencyDTO toDTO(Currency e) {
        return CurrencyDTO.builder()
                .currencyCode(e.getCurrencyCode()).currencyName(e.getCurrencyName())
                .isBaseCurrency(e.getIsBaseCurrency()).activeStatus(e.getActiveStatus())
                .build();
    }

    private ExchangeRateDTO toDTO(ExchangeRate e) {
        return ExchangeRateDTO.builder()
                .id(e.getId()).fromCurrency(e.getFromCurrency()).toCurrency(e.getToCurrency())
                .rate(e.getRate()).effectiveDate(e.getEffectiveDate())
                .build();
    }
}
