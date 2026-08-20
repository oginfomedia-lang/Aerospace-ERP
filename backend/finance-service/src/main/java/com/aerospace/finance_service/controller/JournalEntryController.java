package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.ApprovalActionDTO;
import com.aerospace.finance_service.dto.JournalEntryCreateDTO;
import com.aerospace.finance_service.dto.JournalEntryDTO;
import com.aerospace.finance_service.dto.ReversalRequestDTO;
import com.aerospace.finance_service.service.JournalEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/finance/journals")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @GetMapping
    public ApiResponse<List<JournalEntryDTO>> getAll() {
        return new ApiResponse<>("Journal entries fetched", journalEntryService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<JournalEntryDTO> getById(@PathVariable Long id) {
        return new ApiResponse<>("Journal entry fetched", journalEntryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "JournalEntry")
    public ResponseEntity<ApiResponse<JournalEntryDTO>> create(@Valid @RequestBody JournalEntryCreateDTO dto, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Journal entry created", journalEntryService.create(dto, auth.getName())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "UPDATE", entityType = "JournalEntry")
    public ApiResponse<JournalEntryDTO> update(@PathVariable Long id, @Valid @RequestBody JournalEntryCreateDTO dto) {
        return new ApiResponse<>("Journal entry updated", journalEntryService.update(id, dto));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "SUBMIT", entityType = "JournalEntry")
    public ApiResponse<JournalEntryDTO> submit(@PathVariable Long id, Authentication auth) {
        return new ApiResponse<>("Journal entry submitted", journalEntryService.submit(id, auth.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    @Audited(action = "APPROVE", entityType = "JournalEntry")
    public ApiResponse<JournalEntryDTO> approve(@PathVariable Long id, @RequestBody(required = false) ApprovalActionDTO body, Authentication auth) {
        return new ApiResponse<>("Journal entry approved",
                journalEntryService.approve(id, auth.getName(), rolesOf(auth), body == null ? null : body.getComments()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    @Audited(action = "REJECT", entityType = "JournalEntry")
    public ApiResponse<JournalEntryDTO> reject(@PathVariable Long id, @RequestBody(required = false) ApprovalActionDTO body, Authentication auth) {
        return new ApiResponse<>("Journal entry rejected",
                journalEntryService.reject(id, auth.getName(), rolesOf(auth), body == null ? null : body.getComments()));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    @Audited(action = "REVERSE", entityType = "JournalEntry")
    public ApiResponse<JournalEntryDTO> reverse(@PathVariable Long id, @Valid @RequestBody ReversalRequestDTO body, Authentication auth) {
        return new ApiResponse<>("Reversing journal entry posted", journalEntryService.reverse(id, auth.getName(), body.getReason()));
    }

    // Same role-stripping helper already used in SalesOrderController/NotificationController.
    private Set<String> rolesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toSet());
    }
}
