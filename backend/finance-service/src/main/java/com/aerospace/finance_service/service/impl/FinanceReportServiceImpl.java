package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.FinanceReportSummaryDTO;
import com.aerospace.finance_service.dto.StatementLineDTO;
import com.aerospace.finance_service.dto.TaxReturnReportDTO;
import com.aerospace.finance_service.dto.TrialBalanceLineDTO;
import com.aerospace.finance_service.entity.Account;
import com.aerospace.finance_service.entity.Account.AccountType;
import com.aerospace.finance_service.entity.JournalEntry;
import com.aerospace.finance_service.entity.JournalLine;
import com.aerospace.finance_service.entity.PeriodStatus;
import com.aerospace.finance_service.entity.TaxRate;
import com.aerospace.finance_service.repository.AccountRepository;
import com.aerospace.finance_service.repository.AccountingPeriodRepository;
import com.aerospace.finance_service.repository.JournalEntryRepository;
import com.aerospace.finance_service.repository.JournalLineRepository;
import com.aerospace.finance_service.repository.TaxRateRepository;
import com.aerospace.finance_service.service.CurrencyConversionService;
import com.aerospace.finance_service.service.FinanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceReportServiceImpl implements FinanceReportService {

    private static final String BASE_CURRENCY = "INR";
    private static final String AP_ACCOUNT    = "2000";
    private static final String AR_ACCOUNT    = "1200";

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;
    private final TaxRateRepository taxRateRepository;
    private final CurrencyConversionService currencyConversionService;

    @Override
    public FinanceReportSummaryDTO getSummary(String reportingCurrency) {
        long posted   = journalEntryRepository.countByStatus(JournalEntry.JournalStatus.POSTED);
        long rejected = journalEntryRepository.countByStatus(JournalEntry.JournalStatus.REJECTED);
        long pending  = journalEntryRepository.countByStatus(JournalEntry.JournalStatus.PENDING_APPROVAL);
        long submitted = posted + rejected + pending;
        double successRate = submitted == 0 ? 0.0 : (posted * 100.0) / submitted;

        BigDecimal revenue = sumByAccountType(AccountType.REVENUE);
        BigDecimal expense = sumByAccountType(AccountType.EXPENSE);

        // Operational metrics
        BigDecimal inventoryValue    = sumByAccountCategory(AccountType.ASSET, "Inventory");
        BigDecimal cashPosition      = sumByAccountCategory(AccountType.ASSET, "Cash and Bank");
        BigDecimal mfgCostMTD        = sumExpenseMTD();
        BigDecimal opExpMTD          = sumByAccountTypeMTD(AccountType.EXPENSE);

        FinanceReportSummaryDTO.FinanceReportSummaryDTOBuilder builder = FinanceReportSummaryDTO.builder()
                .totalJournalsSubmitted(submitted).journalsPosted(posted).journalsRejected(rejected)
                .journalPostingSuccessRatePercent(successRate)
                .openPeriodCount(accountingPeriodRepository.countByStatus(PeriodStatus.OPEN))
                .closedPeriodCount(accountingPeriodRepository.countByStatus(PeriodStatus.CLOSED))
                .lockedPeriodCount(accountingPeriodRepository.countByStatus(PeriodStatus.LOCKED))
                .outstandingAP(sumOpenBalance(AP_ACCOUNT))
                .outstandingAR(sumOpenBalance(AR_ACCOUNT))
                .totalRevenuePosted(revenue).totalExpensePosted(expense)
                .grossMarginApprox(revenue.subtract(expense))
                .inventoryValue(inventoryValue)
                .cashPositionApprox(cashPosition)
                .manufacturingCostMTD(mfgCostMTD)
                .operatingExpensesMTD(opExpMTD);

        // Reporting currency conversion (optional)
        if (reportingCurrency != null && !reportingCurrency.isBlank()
                && !reportingCurrency.equalsIgnoreCase(BASE_CURRENCY)) {
            LocalDate today = LocalDate.now();
            BigDecimal revRC  = convertSafely(revenue,  BASE_CURRENCY, reportingCurrency, today);
            BigDecimal expRC  = convertSafely(expense,  BASE_CURRENCY, reportingCurrency, today);
            builder.reportingCurrency(reportingCurrency)
                    .totalRevenueInReportingCurrency(revRC)
                    .totalExpenseInReportingCurrency(expRC)
                    .grossMarginInReportingCurrency(revRC.subtract(expRC));
        }

        return builder.build();
    }

    // ─── GST / VAT Return (Gap 6) ────────────────────────────────────────────

    @Override
    public TaxReturnReportDTO getTaxReturn(LocalDate from, LocalDate to) {
        // Collect all unique tax account codes from the TaxRate table
        List<TaxRate> taxRates = taxRateRepository.findAll();
        Map<String, TaxRate.TaxDirection> codeDirection = taxRates.stream()
                .collect(Collectors.toMap(TaxRate::getTaxAccountCode,
                        TaxRate::getDirection, (a, b) -> a));

        Map<String, BigDecimal> inputByCode  = new HashMap<>();
        Map<String, BigDecimal> outputByCode = new HashMap<>();
        BigDecimal totalInput  = BigDecimal.ZERO;
        BigDecimal totalOutput = BigDecimal.ZERO;

        for (Map.Entry<String, TaxRate.TaxDirection> entry : codeDirection.entrySet()) {
            String accountCode = entry.getKey();
            TaxRate.TaxDirection direction = entry.getValue();

            BigDecimal amount = journalLineRepository
                    .findByAccountCodeAndJournalEntry_Status(accountCode, JournalEntry.JournalStatus.POSTED)
                    .stream()
                    .filter(l -> {
                        LocalDate d = l.getJournalEntry().getPostingDate();
                        return !d.isBefore(from) && !d.isAfter(to);
                    })
                    .map(l -> l.getCreditAmount().subtract(l.getDebitAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (direction == TaxRate.TaxDirection.INPUT) {
                inputByCode.merge(accountCode, amount, BigDecimal::add);
                totalInput = totalInput.add(amount);
            } else {
                outputByCode.merge(accountCode, amount, BigDecimal::add);
                totalOutput = totalOutput.add(amount);
            }
        }

        return TaxReturnReportDTO.builder()
                .fromDate(from).toDate(to)
                .totalInputTax(totalInput.abs())
                .totalOutputTax(totalOutput)
                .netTaxPayable(totalOutput.subtract(totalInput.abs()))
                .inputTaxByRuleCode(inputByCode)
                .outputTaxByRuleCode(outputByCode)
                .build();
    }

    // ─── Trial Balance (Gap 8) ───────────────────────────────────────────────

    @Override
    public List<TrialBalanceLineDTO> getTrialBalance(LocalDate asOf) {
        List<Account> accounts = accountRepository.findAll();
        List<TrialBalanceLineDTO> rows = new ArrayList<>();

        for (Account account : accounts) {
            List<JournalLine> lines = journalLineRepository
                    .findByAccountCodeAndJournalEntry_Status(account.getAccountCode(),
                            JournalEntry.JournalStatus.POSTED)
                    .stream()
                    .filter(l -> !l.getJournalEntry().getPostingDate().isAfter(asOf))
                    .toList();

            if (lines.isEmpty()) continue; // skip zero-activity accounts

            BigDecimal totalDebit  = lines.stream().map(JournalLine::getDebitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredit = lines.stream().map(JournalLine::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            rows.add(TrialBalanceLineDTO.builder()
                    .accountCode(account.getAccountCode())
                    .accountName(account.getAccountName())
                    .accountType(account.getAccountType())
                    .financialCategory(account.getFinancialCategory())
                    .totalDebit(totalDebit).totalCredit(totalCredit)
                    .netBalance(totalDebit.subtract(totalCredit))
                    .build());
        }
        return rows;
    }

    // ─── Profit & Loss (Gap 8) ───────────────────────────────────────────────

    @Override
    public List<StatementLineDTO> getPnL(LocalDate from, LocalDate to) {
        // Include REVENUE and EXPENSE accounts only
        return accountRepository.findAll().stream()
                .filter(a -> a.getAccountType() == AccountType.REVENUE
                          || a.getAccountType() == AccountType.EXPENSE)
                .map(account -> {
                    List<JournalLine> lines = journalLineRepository
                            .findByAccountCodeAndJournalEntry_Status(account.getAccountCode(),
                                    JournalEntry.JournalStatus.POSTED)
                            .stream()
                            .filter(l -> {
                                LocalDate d = l.getJournalEntry().getPostingDate();
                                return !d.isBefore(from) && !d.isAfter(to);
                            }).toList();

                    // Revenue accounts are credit-natural: positive = credit > debit
                    // Expense accounts are debit-natural: positive = debit > credit
                    BigDecimal amount = lines.stream()
                            .map(l -> account.getAccountType() == AccountType.REVENUE
                                    ? l.getCreditAmount().subtract(l.getDebitAmount())
                                    : l.getDebitAmount().subtract(l.getCreditAmount()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return StatementLineDTO.builder()
                            .accountCode(account.getAccountCode())
                            .accountName(account.getAccountName())
                            .category(account.getAccountType() + ": " + account.getFinancialCategory())
                            .amount(amount).build();
                })
                .filter(l -> l.getAmount().compareTo(BigDecimal.ZERO) != 0)
                .toList();
    }

    // ─── Balance Sheet (Gap 8) ───────────────────────────────────────────────

    @Override
    public List<StatementLineDTO> getBalanceSheet(LocalDate asOf) {
        return accountRepository.findAll().stream()
                .filter(a -> a.getAccountType() == AccountType.ASSET
                          || a.getAccountType() == AccountType.LIABILITY
                          || a.getAccountType() == AccountType.EQUITY)
                .map(account -> {
                    List<JournalLine> lines = journalLineRepository
                            .findByAccountCodeAndJournalEntry_Status(account.getAccountCode(),
                                    JournalEntry.JournalStatus.POSTED)
                            .stream()
                            .filter(l -> !l.getJournalEntry().getPostingDate().isAfter(asOf))
                            .toList();

                    // Assets are debit-natural; Liabilities and Equity are credit-natural
                    BigDecimal amount = lines.stream()
                            .map(l -> account.getAccountType() == AccountType.ASSET
                                    ? l.getDebitAmount().subtract(l.getCreditAmount())
                                    : l.getCreditAmount().subtract(l.getDebitAmount()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return StatementLineDTO.builder()
                            .accountCode(account.getAccountCode())
                            .accountName(account.getAccountName())
                            .category(account.getAccountType() + ": " + account.getFinancialCategory())
                            .amount(amount).build();
                })
                .filter(l -> l.getAmount().compareTo(BigDecimal.ZERO) != 0)
                .toList();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private BigDecimal sumOpenBalance(String accountCode) {
        return journalLineRepository
                .findByAccountCodeAndCounterpartyRefIsNotNullAndJournalEntry_Status(
                        accountCode, JournalEntry.JournalStatus.POSTED)
                .stream()
                .map(l -> l.getDebitAmount().subtract(l.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByAccountType(AccountType type) {
        return accountRepository.findAll().stream()
                .filter(a -> a.getAccountType() == type)
                .flatMap(a -> journalLineRepository
                        .findByAccountCodeAndJournalEntry_Status(
                                a.getAccountCode(), JournalEntry.JournalStatus.POSTED).stream())
                .map(l -> l.getCreditAmount().subtract(l.getDebitAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByAccountTypeMTD(AccountType type) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd   = currentMonth.atEndOfMonth();
        return accountRepository.findAll().stream()
                .filter(a -> a.getAccountType() == type)
                .flatMap(a -> journalLineRepository
                        .findByAccountCodeAndJournalEntry_Status(
                                a.getAccountCode(), JournalEntry.JournalStatus.POSTED).stream())
                .filter(l -> {
                    LocalDate d = l.getJournalEntry().getPostingDate();
                    return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
                })
                .map(l -> l.getDebitAmount().subtract(l.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sums posted amounts for ASSET accounts matching a specific financialCategory. */
    private BigDecimal sumByAccountCategory(AccountType type, String category) {
        return accountRepository.findAll().stream()
                .filter(a -> a.getAccountType() == type
                          && category.equalsIgnoreCase(a.getFinancialCategory()))
                .flatMap(a -> journalLineRepository
                        .findByAccountCodeAndJournalEntry_Status(
                                a.getAccountCode(), JournalEntry.JournalStatus.POSTED).stream())
                .map(l -> l.getDebitAmount().subtract(l.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Manufacturing Cost MTD — all EXPENSE account lines for the current month. */
    private BigDecimal sumExpenseMTD() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd   = currentMonth.atEndOfMonth();
        return accountRepository.findAll().stream()
                .filter(a -> a.getAccountType() == AccountType.EXPENSE)
                .flatMap(a -> journalLineRepository
                        .findByAccountCodeAndJournalEntry_Status(
                                a.getAccountCode(), JournalEntry.JournalStatus.POSTED).stream())
                .filter(l -> {
                    LocalDate d = l.getJournalEntry().getPostingDate();
                    return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
                })
                .map(l -> l.getDebitAmount().subtract(l.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal convertSafely(BigDecimal amount, String from, String to, LocalDate asOf) {
        try {
            return currencyConversionService.convert(amount, from, to, asOf);
        } catch (Exception e) {
            return amount; // fallback: return unconverted if rate not available
        }
    }
}
