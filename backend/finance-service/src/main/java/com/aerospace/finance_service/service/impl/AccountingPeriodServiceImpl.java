package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.AccountingPeriodDTO;
import com.aerospace.finance_service.entity.AccountingPeriod;
import com.aerospace.finance_service.entity.FiscalYear;
import com.aerospace.finance_service.entity.PeriodStatus;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.AccountingPeriodRepository;
import com.aerospace.finance_service.repository.FiscalYearRepository;
import com.aerospace.finance_service.service.AccountingPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingPeriodServiceImpl implements AccountingPeriodService {

    private final AccountingPeriodRepository periodRepository;
    private final FiscalYearRepository fiscalYearRepository;

    /**
     * Central "may I post today?" check. This is the single centralized enforcement
     * point (see Phase 11's security notes) — it deliberately lives here, not
     * duplicated in JournalEntryService or PostingEngine, so exactly one method in the
     * whole codebase can ever say "yes, post it."
     */
    @Override
    public AccountingPeriod assertOpenForPosting(LocalDate postingDate) {
        AccountingPeriod period = periodRepository.findPeriodContaining(postingDate)
                .orElseThrow(() -> new IllegalStateException("No accounting period defined covering date " + postingDate));
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new IllegalStateException("Accounting period " + period.getPeriodLabel() +
                    " is " + period.getStatus() + " — postings are not allowed");
        }
        return period;
    }

    /** Splits a fiscal year's start/end range into 12 monthly OPEN periods. */
    @Override
    @Transactional
    public List<AccountingPeriodDTO> generateMonthlyPeriods(Long fiscalYearId) {
        FiscalYear fy = fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiscal year not found: " + fiscalYearId));

        List<AccountingPeriod> periods = new ArrayList<>();
        LocalDate cursor = fy.getStartDate();
        int periodNumber = 1;
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("yyyy-MM");

        while (!cursor.isAfter(fy.getEndDate()) && periodNumber <= 12) {
            LocalDate periodEnd = cursor.withDayOfMonth(cursor.lengthOfMonth());
            if (periodEnd.isAfter(fy.getEndDate())) periodEnd = fy.getEndDate();

            periods.add(AccountingPeriod.builder()
                    .fiscalYear(fy)
                    .periodNumber(periodNumber)
                    .periodLabel(cursor.format(labelFormat))
                    .startDate(cursor)
                    .endDate(periodEnd)
                    .status(PeriodStatus.OPEN)
                    .build());

            cursor = periodEnd.plusDays(1);
            periodNumber++;
        }

        return periodRepository.saveAll(periods).stream().map(this::toDTO).toList();
    }

    /** Closing does NOT touch already-posted journals — those stay immutable regardless of period state (ADR-DDS-057). */
    @Override
    @Transactional
    public AccountingPeriodDTO close(Long periodId) {
        AccountingPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found: " + periodId));
        if (period.getStatus() == PeriodStatus.LOCKED) {
            throw new IllegalStateException("Period is locked and already permanently closed");
        }
        period.setStatus(PeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(currentUsername());
        return toDTO(periodRepository.save(period));
    }

    /** Reopen a CLOSED period. Controller @PreAuthorize restricts this to FINANCE_MANAGER/ADMIN ("authorized users only"). */
    @Override
    @Transactional
    public AccountingPeriodDTO reopen(Long periodId) {
        AccountingPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found: " + periodId));
        if (period.getStatus() == PeriodStatus.LOCKED) {
            throw new IllegalStateException("Locked periods cannot be reopened directly — unlock first");
        }
        if (period.getStatus() == PeriodStatus.OPEN) {
            throw new IllegalStateException("Period is already open");
        }
        period.setStatus(PeriodStatus.OPEN);
        period.setReopenedAt(LocalDateTime.now());
        period.setReopenedBy(currentUsername());
        return toDTO(periodRepository.save(period));
    }

    /** Locking = the permanent "never touch again" state for old historical periods. ADMIN only. */
    @Override
    @Transactional
    public AccountingPeriodDTO lock(Long periodId) {
        AccountingPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found: " + periodId));
        period.setStatus(PeriodStatus.LOCKED);
        return toDTO(periodRepository.save(period));
    }

    @Override
    @Transactional
    public AccountingPeriodDTO unlock(Long periodId) {
        AccountingPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found: " + periodId));
        if (period.getStatus() != PeriodStatus.LOCKED) {
            throw new IllegalStateException("Period is not locked");
        }
        period.setStatus(PeriodStatus.CLOSED); // drops back to CLOSED, not straight to OPEN
        return toDTO(periodRepository.save(period));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    @Override
    public List<AccountingPeriodDTO> getAll() {
        return periodRepository.findAll().stream().map(this::toDTO).toList();
    }

    private AccountingPeriodDTO toDTO(AccountingPeriod p) {
        return AccountingPeriodDTO.builder()
                .id(p.getId()).fiscalYearCode(p.getFiscalYear().getFiscalYearCode())
                .periodNumber(p.getPeriodNumber()).periodLabel(p.getPeriodLabel())
                .startDate(p.getStartDate()).endDate(p.getEndDate()).status(p.getStatus())
                .closedAt(p.getClosedAt()).closedBy(p.getClosedBy()).build();
    }
}
