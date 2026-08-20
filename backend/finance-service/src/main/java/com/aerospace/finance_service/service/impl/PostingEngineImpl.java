package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.JournalEntryCreateDTO;
import com.aerospace.finance_service.dto.JournalEntryDTO;
import com.aerospace.finance_service.dto.JournalLineCreateDTO;
import com.aerospace.finance_service.dto.TaxCalculationResult;
import com.aerospace.finance_service.entity.PostingRule;
import com.aerospace.finance_service.entity.PostingTriggerType;
import com.aerospace.finance_service.entity.TaxRate;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.PostingRuleRepository;
import com.aerospace.finance_service.service.JournalEntryService;
import com.aerospace.finance_service.service.PostingEngine;
import com.aerospace.finance_service.service.TaxCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostingEngineImpl implements PostingEngine {

    private final PostingRuleRepository postingRuleRepository;
    private final TaxCalculator taxCalculator;
    private final JournalEntryService journalEntryService;
    private static final String SYSTEM_USER = "posting-engine";

    /**
     * The single entry point every business-event consumer calls. Given a trigger type
     * and an amount, it: (1) looks up the configured PostingRule for that trigger —
     * fails loudly if none, a missing rule should never silently swallow money; (2)
     * builds a balanced 2-line journal entry from the rule's debit/credit accounts;
     * (3) if the rule references a tax rule, asks TaxCalculator for the split and adds
     * a third (or fourth, for reverse charge) line; (4) submits it — JournalEntryService
     * decides auto-post vs approval routing based on the configured threshold, exactly
     * like a manually created journal would.
     */
    @Override
    public JournalEntryDTO postFromEvent(PostingTriggerType triggerEvent, BigDecimal amount, String currency,
                                          String sourceDocType, String sourceDocRef,
                                          Long costCenterId, Long profitCenterId,
                                          Long supplierId, Long customerId) {

        PostingRule rule = postingRuleRepository.findByTriggerEventAndIsActiveTrue(triggerEvent)
                .orElseThrow(() -> new ResourceNotFoundException("No active posting rule configured for trigger event: " + triggerEvent));

        Long effectiveCostCenter = costCenterId != null ? costCenterId : rule.getCostCenterId();
        Long effectiveProfitCenter = profitCenterId != null ? profitCenterId : rule.getProfitCenterId();

        List<JournalLineCreateDTO> lines = new ArrayList<>();

        if (rule.getTaxRuleCode() != null && !rule.getTaxRuleCode().isBlank()) {
            TaxCalculationResult tax = taxCalculator.calculate(rule.getTaxRuleCode(), amount);
            if (!tax.isReverseCharge()) {
                lines.add(JournalLineCreateDTO.builder()
                        .accountCode(tax.getTaxAccountCode())
                        .debitAmount(tax.getDirection() == TaxRate.TaxDirection.INPUT ? tax.getTaxAmount() : BigDecimal.ZERO)
                        .creditAmount(tax.getDirection() == TaxRate.TaxDirection.OUTPUT ? tax.getTaxAmount() : BigDecimal.ZERO)
                        .currency(currency).costCenterId(effectiveCostCenter).profitCenterId(effectiveProfitCenter)
                        .description("Tax: " + rule.getTaxRuleCode()).build());
            } else {
                lines.add(JournalLineCreateDTO.builder()
                        .accountCode(tax.getTaxAccountCode()).debitAmount(tax.getTaxAmount())
                        .currency(currency).costCenterId(effectiveCostCenter).profitCenterId(effectiveProfitCenter)
                        .description("Reverse charge input tax: " + rule.getTaxRuleCode()).build());
                lines.add(JournalLineCreateDTO.builder()
                        .accountCode(tax.getReverseChargeCounterAccountCode()).creditAmount(tax.getTaxAmount())
                        .currency(currency).costCenterId(effectiveCostCenter).profitCenterId(effectiveProfitCenter)
                        .description("Reverse charge output tax (self-assessed): " + rule.getTaxRuleCode()).build());
            }
        }

        // Debit side = AR/customer by convention (the two only trigger types that ever
        // carry a counterparty), credit side = AP/supplier — hardcoded rather than a
        // generic PostingRule column, since only VENDOR_INVOICE_APPROVED and
        // CUSTOMER_INVOICE ever pass a non-null supplierId/customerId here.
        JournalLineCreateDTO.JournalLineCreateDTOBuilder debitLine = JournalLineCreateDTO.builder()
                .accountCode(rule.getDebitAccountCode()).debitAmount(amount).currency(currency)
                .costCenterId(effectiveCostCenter).profitCenterId(effectiveProfitCenter)
                .description("Auto-posted: " + triggerEvent);
        if (customerId != null) debitLine.customerId(customerId);
        lines.add(debitLine.build());

        JournalLineCreateDTO.JournalLineCreateDTOBuilder creditLine = JournalLineCreateDTO.builder()
                .accountCode(rule.getCreditAccountCode()).creditAmount(amount).currency(currency)
                .costCenterId(effectiveCostCenter).profitCenterId(effectiveProfitCenter)
                .description("Auto-posted: " + triggerEvent);
        if (supplierId != null) creditLine.supplierId(supplierId);
        lines.add(creditLine.build());

        JournalEntryCreateDTO entryDto = JournalEntryCreateDTO.builder()
                .postingDate(LocalDate.now())
                .narration("Auto-posted from event " + triggerEvent + " (" + sourceDocRef + ")")
                .sourceDocType(sourceDocType).sourceDocRef(sourceDocRef)
                .costCenterId(effectiveCostCenter).profitCenterId(effectiveProfitCenter)
                .lines(lines).build();

        JournalEntryDTO created = journalEntryService.create(entryDto, SYSTEM_USER);
        return journalEntryService.submit(created.getId(), SYSTEM_USER);
    }
}
