package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.RevaluationRequestDTO;
import com.aerospace.finance_service.service.RevaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes the FX revaluation engine via REST. Run this at month-end, BEFORE
 * closing the period, to book unrealized FX gain/loss on open foreign-currency
 * AP/AR balances.
 */
@RestController
@RequestMapping("/api/v1/finance/revaluation")
@RequiredArgsConstructor
public class RevaluationController {

    private final RevaluationService revaluationService;

    /**
     * Trigger FX revaluation for a specific control account and foreign currency.
     * Example body: { "controlAccountCode": "2000", "foreignCurrency": "USD", "asOfDate": "2026-06-30" }
     * Response: success message (the revaluation journal entry is visible in /api/v1/finance/journals)
     */
    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN','MANAGER')")
    @Audited(action = "RUN_REVALUATION", entityType = "JournalEntry")
    public ApiResponse<String> runRevaluation(
            @Valid @RequestBody RevaluationRequestDTO req, Authentication auth) {
        revaluationService.revalueForeignCurrencyBalances(
                req.getControlAccountCode(),
                req.getForeignCurrency(),
                req.getAsOfDate(),
                auth.getName());
        return new ApiResponse<>("FX revaluation completed",
                "Revaluation journal posted for account " + req.getControlAccountCode()
                        + " (" + req.getForeignCurrency() + ") as of " + req.getAsOfDate());
    }
}
