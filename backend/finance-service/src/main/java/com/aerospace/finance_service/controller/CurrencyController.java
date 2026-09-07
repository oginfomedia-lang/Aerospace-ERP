package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.CurrencyDTO;
import com.aerospace.finance_service.dto.ExchangeRateDTO;
import com.aerospace.finance_service.service.CurrencyService;
import com.aerospace.finance_service.service.RevaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;
    private final RevaluationService revaluationService;

    @GetMapping("/currencies")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    public ApiResponse<List<CurrencyDTO>> getCurrencies() {
        return new ApiResponse<>("Currencies fetched", currencyService.getAllCurrencies());
    }

    @PostMapping("/currencies")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "Currency")
    public ResponseEntity<ApiResponse<CurrencyDTO>> createCurrency(@Valid @RequestBody CurrencyDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Currency created", currencyService.createCurrency(dto)));
    }

    @GetMapping("/exchange-rates")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    public ApiResponse<List<ExchangeRateDTO>> getExchangeRates() {
        return new ApiResponse<>("Exchange rates fetched", currencyService.getAllExchangeRates());
    }

    @PostMapping("/exchange-rates")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "ExchangeRate")
    public ResponseEntity<ApiResponse<ExchangeRateDTO>> createExchangeRate(@Valid @RequestBody ExchangeRateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Exchange rate created", currencyService.createExchangeRate(dto)));
    }

    @PostMapping("/revalue")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "REVALUE", entityType = "ExchangeRate")
    public ApiResponse<Void> revalue(@RequestParam String controlAccountCode, @RequestParam String foreignCurrency,
                                      @RequestParam LocalDate asOfDate, Authentication auth) {
        revaluationService.revalueForeignCurrencyBalances(controlAccountCode, foreignCurrency, asOfDate, auth.getName());
        return new ApiResponse<>("Revaluation complete", null);
    }
}
