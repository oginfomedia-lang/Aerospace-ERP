package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.JournalEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findByJournalNumber(String journalNumber);
    long countByStatus(JournalEntry.JournalStatus status);
    List<JournalEntry> findByStatus(JournalEntry.JournalStatus status);
    Optional<JournalEntry> findBySourceDocTypeAndSourceDocRef(String sourceDocType, String sourceDocRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JournalEntry j where j.id = :id")
    Optional<JournalEntry> lockById(@Param("id") Long id);
}
