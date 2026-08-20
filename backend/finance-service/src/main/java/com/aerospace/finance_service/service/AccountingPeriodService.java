package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.AccountingPeriodDTO;
import com.aerospace.finance_service.entity.AccountingPeriod;

import java.time.LocalDate;
import java.util.List;

public interface AccountingPeriodService {

    /**
     * Central "may I post today?" check. EVERY code path that writes a JournalEntry
     * must call this first — see JournalEntryServiceImpl.create() and
     * PostingEngineImpl.postFromEvent() (via JournalEntryService.create()).
     */
    AccountingPeriod assertOpenForPosting(LocalDate postingDate);

    List<AccountingPeriodDTO> generateMonthlyPeriods(Long fiscalYearId);
    List<AccountingPeriodDTO> getAll();
    AccountingPeriodDTO close(Long periodId);
    AccountingPeriodDTO reopen(Long periodId);
    AccountingPeriodDTO lock(Long periodId);
    AccountingPeriodDTO unlock(Long periodId);
}
