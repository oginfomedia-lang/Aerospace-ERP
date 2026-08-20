package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.JournalEntry;
import com.aerospace.finance_service.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
    boolean existsByAccountCode(String accountCode);
    List<JournalLine> findByAccountCode(String accountCode);
    List<JournalLine> findByAccountCodeAndCounterpartyRefIsNotNull(String accountCode);
    List<JournalLine> findByAccountCodeAndJournalEntry_PostingDateBetween(String accountCode, LocalDate from, LocalDate to);

    // Real balances/postings must only reflect committed (POSTED) entries — a DRAFT or
    // PENDING_APPROVAL journal is not yet a fact of the books and must not move a
    // reported balance, aging bucket, or revaluation base.
    List<JournalLine> findByAccountCodeAndJournalEntry_Status(String accountCode, JournalEntry.JournalStatus status);
    List<JournalLine> findByAccountCodeAndCounterpartyRefIsNotNullAndJournalEntry_Status(String accountCode, JournalEntry.JournalStatus status);
}
