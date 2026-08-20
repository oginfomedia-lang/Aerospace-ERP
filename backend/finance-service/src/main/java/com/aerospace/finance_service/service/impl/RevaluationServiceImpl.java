package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.JournalEntryCreateDTO;
import com.aerospace.finance_service.dto.JournalLineCreateDTO;
import com.aerospace.finance_service.entity.JournalEntry;
import com.aerospace.finance_service.entity.JournalLine;
import com.aerospace.finance_service.repository.JournalLineRepository;
import com.aerospace.finance_service.service.CurrencyConversionService;
import com.aerospace.finance_service.service.JournalEntryService;
import com.aerospace.finance_service.service.RevaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Foreign-currency revaluation: recomputes the base-currency value of open
 * (unsettled) foreign-currency balances using today's rate, and books the
 * FX gain/loss delta. Run on demand or on a monthly schedule at period-end,
 * BEFORE closing the period.
 *
 * Scope note: "open balance" here is approximated as the net of all journal
 * lines posted against a given foreign-currency AP/AR control account that
 * have not yet been matched by a settlement line referencing the same
 * counterpartyRef. A full open-item ledger is out of scope for this pass -
 * see the AP/AR aging note for the same simplification.
 */
@Service
@RequiredArgsConstructor
public class RevaluationServiceImpl implements RevaluationService {

    private static final String FX_GAIN_LOSS_ACCOUNT = "7900"; // "Unrealized FX Gain/Loss"
    private static final String BASE_CURRENCY = "INR";

    private final JournalLineRepository journalLineRepository;
    private final CurrencyConversionService currencyConversionService;
    private final JournalEntryService journalEntryService;

    @Override
    public void revalueForeignCurrencyBalances(String controlAccountCode, String foreignCurrency, LocalDate asOfDate, String runByUser) {
        List<JournalLine> lines = journalLineRepository.findByAccountCodeAndCounterpartyRefIsNotNullAndJournalEntry_Status(controlAccountCode, JournalEntry.JournalStatus.POSTED)
                .stream().filter(l -> l.getCurrency().equals(foreignCurrency)).toList();

        Map<String, BigDecimal> openByCounterparty = lines.stream()
                .collect(Collectors.groupingBy(JournalLine::getCounterpartyRef,
                        Collectors.reducing(BigDecimal.ZERO, l -> l.getDebitAmount().subtract(l.getCreditAmount()), BigDecimal::add)));

        BigDecimal currentRate = currencyConversionService.getRate(foreignCurrency, BASE_CURRENCY, asOfDate);
        BigDecimal totalDelta = BigDecimal.ZERO;
        List<JournalLineCreateDTO> revalLines = new ArrayList<>();

        for (var entry : openByCounterparty.entrySet()) {
            BigDecimal foreignOpenBalance = entry.getValue();
            if (foreignOpenBalance.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal originalBaseValue = lines.stream()
                    .filter(l -> l.getCounterpartyRef().equals(entry.getKey()))
                    .map(l -> currencyConversionService.convert(l.getDebitAmount().subtract(l.getCreditAmount()),
                            foreignCurrency, BASE_CURRENCY, l.getJournalEntry().getPostingDate()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal newBaseValue = foreignOpenBalance.multiply(currentRate).setScale(2, RoundingMode.HALF_UP);
            // delta is expressed directly in (debit - credit) terms, the same convention
            // foreignOpenBalance already uses — so delta > 0 always means "debit the
            // control account by delta," regardless of whether this account naturally
            // runs a debit balance (a receivable) or a credit balance (a payable, where
            // foreignOpenBalance itself is negative). Do NOT reason about this in terms
            // of "grew = loss" — that mapping flips depending on receivable vs payable;
            // letting the debit/credit branches follow the sign of delta directly is
            // what keeps both cases correct.
            BigDecimal delta = newBaseValue.subtract(originalBaseValue);
            if (delta.compareTo(BigDecimal.ZERO) == 0) continue;

            revalLines.add(JournalLineCreateDTO.builder()
                    .accountCode(controlAccountCode)
                    .debitAmount(delta.compareTo(BigDecimal.ZERO) > 0 ? delta : BigDecimal.ZERO)
                    .creditAmount(delta.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ZERO : delta.abs())
                    .currency(BASE_CURRENCY).counterpartyRef(entry.getKey())
                    .description("FX revaluation @ " + currentRate).build());
            totalDelta = totalDelta.add(delta);
        }

        if (totalDelta.compareTo(BigDecimal.ZERO) == 0) return; // nothing moved, no entry needed

        // Offsetting line: the opposite side of whatever was just posted to the control
        // account(s) above, so the entry balances. A net debit to the control accounts
        // (totalDelta > 0) is credited here — a gain; a net credit (totalDelta < 0) is
        // debited here — a loss. This is what correctly produces "debit FX Loss, credit
        // a growing payable" and "credit FX Gain, debit a growing receivable."
        revalLines.add(JournalLineCreateDTO.builder()
                .accountCode(FX_GAIN_LOSS_ACCOUNT)
                .debitAmount(totalDelta.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ZERO : totalDelta.abs())
                .creditAmount(totalDelta.compareTo(BigDecimal.ZERO) > 0 ? totalDelta : BigDecimal.ZERO)
                .currency(BASE_CURRENCY).description("Net unrealized FX gain/loss").build());

        JournalEntryCreateDTO revalEntry = JournalEntryCreateDTO.builder()
                .postingDate(asOfDate)
                .narration("FX revaluation of " + controlAccountCode + " (" + foreignCurrency + ") as of " + asOfDate)
                .sourceDocType("FX_REVALUATION").sourceDocRef(controlAccountCode + "-" + asOfDate)
                .lines(revalLines).build();

        var created = journalEntryService.create(revalEntry, runByUser);
        journalEntryService.submit(created.getId(), runByUser); // auto-posts if under threshold, else queues approval
    }
}
