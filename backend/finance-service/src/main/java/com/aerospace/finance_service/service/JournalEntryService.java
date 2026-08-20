package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.JournalEntryCreateDTO;
import com.aerospace.finance_service.dto.JournalEntryDTO;

import java.util.List;
import java.util.Set;

public interface JournalEntryService {
    JournalEntryDTO create(JournalEntryCreateDTO dto, String currentUser);
    JournalEntryDTO getById(Long id);
    List<JournalEntryDTO> getAll();
    JournalEntryDTO update(Long id, JournalEntryCreateDTO dto);
    JournalEntryDTO submit(Long id, String currentUser);
    JournalEntryDTO approve(Long id, String approverUsername, Set<String> approverRoles, String remarks);
    JournalEntryDTO reject(Long id, String approverUsername, Set<String> approverRoles, String remarks);
    JournalEntryDTO reverse(Long id, String currentUser, String reason);
}
