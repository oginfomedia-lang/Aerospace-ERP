package com.aerospace.finance_service.controller;

import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.AgingBucketDTO;
import com.aerospace.finance_service.dto.LedgerViewDTO;
import com.aerospace.finance_service.service.LedgerViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/ar")
@RequiredArgsConstructor
public class AccountsReceivableController {

    private static final String AR_CONTROL_ACCOUNT = "1200";
    private final LedgerViewService ledgerViewService;

    @GetMapping("/ledger")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','MANAGER','ADMIN')")
    public ApiResponse<List<LedgerViewDTO>> getCustomerLedger() {
        return new ApiResponse<>("Customer ledger fetched", ledgerViewService.getLedger(AR_CONTROL_ACCOUNT));
    }

    @GetMapping("/aging")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','MANAGER','ADMIN')")
    public ApiResponse<List<AgingBucketDTO>> getAging(@RequestParam(required = false) LocalDate asOfDate) {
        return new ApiResponse<>("AR aging fetched", ledgerViewService.getAging(AR_CONTROL_ACCOUNT, asOfDate == null ? LocalDate.now() : asOfDate));
    }
}
