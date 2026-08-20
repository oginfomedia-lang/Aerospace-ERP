package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.CurrencyDTO;
import com.aerospace.finance_service.dto.ExchangeRateDTO;

import java.util.List;

public interface CurrencyService {
    CurrencyDTO createCurrency(CurrencyDTO dto);
    List<CurrencyDTO> getAllCurrencies();
    ExchangeRateDTO createExchangeRate(ExchangeRateDTO dto);
    List<ExchangeRateDTO> getAllExchangeRates();
}
