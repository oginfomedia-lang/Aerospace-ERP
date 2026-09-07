package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.AccountingPeriodDTO;
import com.aerospace.finance_service.service.AccountingPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService periodService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    public ApiResponse<List<AccountingPeriodDTO>> getAll() {
        return new ApiResponse<>("Accounting periods fetched", periodService.getAll());
    }

    @PostMapping("/fiscal-years/{fiscalYearId}/generate-monthly")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    @Audited(action = "GENERATE_PERIODS", entityType = "AccountingPeriod")
    public ApiResponse<List<AccountingPeriodDTO>> generateMonthlyPeriods(@PathVariable Long fiscalYearId) {
        return new ApiResponse<>("Monthly periods generated", periodService.generateMonthlyPeriods(fiscalYearId));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    @Audited(action = "CLOSE_PERIOD", entityType = "AccountingPeriod")
    public ApiResponse<AccountingPeriodDTO> close(@PathVariable Long id) {
        return new ApiResponse<>("Period closed", periodService.close(id));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    @Audited(action = "REOPEN_PERIOD", entityType = "AccountingPeriod")
    public ApiResponse<AccountingPeriodDTO> reopen(@PathVariable Long id) {
        return new ApiResponse<>("Period reopened", periodService.reopen(id));
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "LOCK_PERIOD", entityType = "AccountingPeriod")
    public ApiResponse<AccountingPeriodDTO> lock(@PathVariable Long id) {
        return new ApiResponse<>("Period locked", periodService.lock(id));
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "UNLOCK_PERIOD", entityType = "AccountingPeriod")
    public ApiResponse<AccountingPeriodDTO> unlock(@PathVariable Long id) {
        return new ApiResponse<>("Period unlocked", periodService.unlock(id));
    }
}
