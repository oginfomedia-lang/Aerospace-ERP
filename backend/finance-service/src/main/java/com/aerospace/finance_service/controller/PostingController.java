package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.JournalEntryDTO;
import com.aerospace.finance_service.dto.ManualPostingRequestDTO;
import com.aerospace.finance_service.dto.PostingRuleDTO;
import com.aerospace.finance_service.service.PostingEngine;
import com.aerospace.finance_service.service.PostingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/posting")
@RequiredArgsConstructor
public class PostingController {

    private final PostingEngine postingEngine;
    private final PostingRuleService postingRuleService;

    // manual/test trigger — matches the spec's POST /api/v1/finance/posting/run literally
    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    @Audited(action = "MANUAL_POST", entityType = "JournalEntry")
    public ApiResponse<JournalEntryDTO> run(@Valid @RequestBody ManualPostingRequestDTO req) {
        return new ApiResponse<>("Posting run complete", postingEngine.postFromEvent(
                req.getTriggerEvent(), req.getAmount(), req.getCurrency(),
                req.getSourceDocType(), req.getSourceDocRef(), req.getCostCenterId(), req.getProfitCenterId()));
    }

    @GetMapping("/rules")
    public ApiResponse<List<PostingRuleDTO>> getRules() {
        return new ApiResponse<>("Posting rules fetched", postingRuleService.getAll());
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    @Audited(action = "CREATE", entityType = "PostingRule")
    public ApiResponse<PostingRuleDTO> createRule(@Valid @RequestBody PostingRuleDTO dto) {
        return new ApiResponse<>("Posting rule created", postingRuleService.create(dto));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN', 'MANAGER')")
    @Audited(action = "UPDATE", entityType = "PostingRule")
    public ApiResponse<PostingRuleDTO> updateRule(@PathVariable Long id, @Valid @RequestBody PostingRuleDTO dto) {
        return new ApiResponse<>("Posting rule updated", postingRuleService.update(id, dto));
    }
}
