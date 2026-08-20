package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.FinanceReportSummaryDTO;
import com.aerospace.finance_service.dto.StatementLineDTO;
import com.aerospace.finance_service.dto.TaxReturnReportDTO;
import com.aerospace.finance_service.dto.TrialBalanceLineDTO;

import java.time.LocalDate;
import java.util.List;

public interface FinanceReportService {
    FinanceReportSummaryDTO getSummary(String reportingCurrency);
    TaxReturnReportDTO getTaxReturn(LocalDate from, LocalDate to);
    List<TrialBalanceLineDTO> getTrialBalance(LocalDate asOf);
    List<StatementLineDTO> getPnL(LocalDate from, LocalDate to);
    List<StatementLineDTO> getBalanceSheet(LocalDate asOf);
}
