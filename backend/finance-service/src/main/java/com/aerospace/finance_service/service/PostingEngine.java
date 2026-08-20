package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.JournalEntryDTO;
import com.aerospace.finance_service.entity.PostingTriggerType;

import java.math.BigDecimal;

public interface PostingEngine {

    /**
     * The single entry point every business-event consumer (and the manual
     * POST /posting/run endpoint) calls. Given a trigger type and an amount, it looks
     * up the configured PostingRule, builds a balanced journal entry (with a tax line
     * if the rule references one), and submits it. supplierId/customerId are optional
     * — only VENDOR_INVOICE_APPROVED (supplier) and CUSTOMER_INVOICE (customer) trigger
     * types ever have a real counterparty; passing one tags the relevant line so it
     * shows up correctly in the AP/AR ledger and aging views instead of being invisible.
     */
    JournalEntryDTO postFromEvent(PostingTriggerType triggerEvent, BigDecimal amount, String currency,
                                  String sourceDocType, String sourceDocRef,
                                  Long costCenterId, Long profitCenterId,
                                  Long supplierId, Long customerId);

    // Convenience overload for the 6 trigger types with no counterparty concept at all
    // (inventory adjustment, maintenance cost, quality cost, finished goods receipt,
    // material consumption, purchase receipt) — no call-site changes needed for those.
    default JournalEntryDTO postFromEvent(PostingTriggerType triggerEvent, BigDecimal amount, String currency,
                                          String sourceDocType, String sourceDocRef,
                                          Long costCenterId, Long profitCenterId) {
        return postFromEvent(triggerEvent, amount, currency, sourceDocType, sourceDocRef,
                costCenterId, profitCenterId, null, null);
    }
}
