package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.JournalEntryDTO;
import com.aerospace.finance_service.dto.LedgerViewDTO;
import com.aerospace.finance_service.entity.PostingTriggerType;
import com.aerospace.finance_service.service.LedgerViewService;
import com.aerospace.finance_service.service.PostingEngine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/ar/invoices")
@RequiredArgsConstructor
public class CustomerInvoiceController {

    private static final String AR_CONTROL_ACCOUNT = "1200";

    private final PostingEngine postingEngine;
    private final LedgerViewService ledgerViewService;

    /**
     * Manual trigger for posting a customer invoice journal — mirrors
     * POST /api/v1/finance/posting/run but specifically for AR/customer invoice context.
     * Used for testing or manual AR posting when the Kafka event is not available.
     */
    @PostMapping("/post")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    @Audited(action = "POST_CUSTOMER_INVOICE", entityType = "JournalEntry")
    public ApiResponse<JournalEntryDTO> postInvoice(
            @Valid @RequestBody ManualARPostRequest req, Authentication auth) {
        JournalEntryDTO result = postingEngine.postFromEvent(
                PostingTriggerType.CUSTOMER_INVOICE,
                req.getAmount(), req.getCurrency(),
                "CUSTOMER_INVOICE", req.getInvoiceNumber(), null, null, null, req.getCustomerId());
        return new ApiResponse<>("Customer invoice posted to AR", result);
    }

    /** View all AR ledger entries (customer receivable balances). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<List<LedgerViewDTO>> getARLedger() {
        return new ApiResponse<>("AR ledger fetched",
                ledgerViewService.getLedger(AR_CONTROL_ACCOUNT));
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ManualARPostRequest {
        @NotBlank private String invoiceNumber;
        @NotNull @Positive private BigDecimal amount;
        @NotBlank private String currency;
        @NotNull private Long customerId;
    }
}
