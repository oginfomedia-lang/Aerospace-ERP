package com.aerospace.finance_service.service;

import java.time.LocalDate;

public interface RevaluationService {
    void revalueForeignCurrencyBalances(String controlAccountCode, String foreignCurrency, LocalDate asOfDate, String runByUser);
}
