package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.AccountDTO;
import com.aerospace.finance_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ApiResponse<List<AccountDTO>> getAll() {
        return new ApiResponse<>("Accounts fetched", accountService.getAll());
    }

    @GetMapping("/tree")
    public ApiResponse<List<AccountDTO>> getTree() {
        return new ApiResponse<>("Chart of accounts tree fetched", accountService.getTree());
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountDTO> getById(@PathVariable Long id) {
        return new ApiResponse<>("Account fetched", accountService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "Account")
    public ResponseEntity<ApiResponse<AccountDTO>> create(@Valid @RequestBody AccountDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Account created", accountService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "UPDATE", entityType = "Account")
    public ApiResponse<AccountDTO> update(@PathVariable Long id, @Valid @RequestBody AccountDTO dto) {
        return new ApiResponse<>("Account updated", accountService.update(id, dto));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "DEACTIVATE", entityType = "Account")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        accountService.deactivate(id);
        return new ApiResponse<>("Account deactivated", null);
    }
}
