package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.JournalEntryCreateDTO;
import com.aerospace.finance_service.dto.JournalEntryDTO;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface JournalEntryService {
    JournalEntryDTO create(JournalEntryCreateDTO dto, String currentUser);
    JournalEntryDTO getById(Long id);
    List<JournalEntryDTO> getAll();
    // Kafka posting-consumer idempotency lookup: lets callers check for an already-posted
    // journal for a given source document before creating a duplicate one.
    Optional<JournalEntryDTO> findBySourceDoc(String sourceDocType, String sourceDocRef);
    JournalEntryDTO update(Long id, JournalEntryCreateDTO dto);
    JournalEntryDTO submit(Long id, String currentUser);
    JournalEntryDTO approve(Long id, String approverUsername, Set<String> approverRoles, String remarks);
    JournalEntryDTO reject(Long id, String approverUsername, Set<String> approverRoles, String remarks);
    JournalEntryDTO reverse(Long id, String currentUser, String reason);
}
