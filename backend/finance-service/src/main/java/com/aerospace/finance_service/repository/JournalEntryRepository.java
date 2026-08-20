package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findByJournalNumber(String journalNumber);
    long countByStatus(JournalEntry.JournalStatus status);
    List<JournalEntry> findByStatus(JournalEntry.JournalStatus status);
}
