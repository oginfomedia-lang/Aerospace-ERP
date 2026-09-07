package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.FinanceReportSummaryDTO;
import com.aerospace.finance_service.dto.StatementLineDTO;
import com.aerospace.finance_service.dto.TaxReturnReportDTO;
import com.aerospace.finance_service.dto.TrialBalanceLineDTO;
import com.aerospace.finance_service.service.FinanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/reports")
@RequiredArgsConstructor
public class FinanceReportController {

    private final FinanceReportService financeReportService;

    /**
     * Overall finance summary dashboard.
     * Optional: ?reportingCurrency=USD to see amounts converted to a 3rd currency.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<FinanceReportSummaryDTO> getSummary(
            @RequestParam(required = false) String reportingCurrency) {
        return new ApiResponse<>("Finance summary fetched",
                financeReportService.getSummary(reportingCurrency));
    }

    /**
     * GST/VAT Return Report — totals input and output tax for the given date range.
     * Example: GET /api/v1/finance/reports/tax-return?from=2026-04-01&to=2026-06-30
     */
    @GetMapping("/tax-return")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<TaxReturnReportDTO> getTaxReturn(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return new ApiResponse<>("Tax return report fetched",
                financeReportService.getTaxReturn(from, to));
    }

    /**
     * Trial Balance — all accounts with their total debits, credits, and net balance.
     * asOf defaults to today if not provided.
     */
    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<List<TrialBalanceLineDTO>> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return new ApiResponse<>("Trial balance fetched",
                financeReportService.getTrialBalance(asOf == null ? LocalDate.now() : asOf));
    }

    /**
     * Profit & Loss Statement — revenue and expense accounts for a date range.
     * Example: GET /api/v1/finance/reports/pnl?from=2026-04-01&to=2026-06-30
     */
    @GetMapping("/pnl")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<List<StatementLineDTO>> getPnL(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return new ApiResponse<>("Profit & Loss report fetched",
                financeReportService.getPnL(from, to));
    }

    /**
     * Balance Sheet — asset, liability, and equity accounts as of a date.
     * Example: GET /api/v1/finance/reports/balance-sheet?asOf=2026-06-30
     */
    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<List<StatementLineDTO>> getBalanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return new ApiResponse<>("Balance sheet fetched",
                financeReportService.getBalanceSheet(asOf == null ? LocalDate.now() : asOf));
    }
}
