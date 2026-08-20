package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.JournalEntryDTO;
import com.aerospace.finance_service.entity.PostingTriggerType;

import java.math.BigDecimal;

public interface PostingEngine {

    /**
     * The single entry point every business-event consumer (and the manual
     * POST /posting/run endpoint) calls. Given a trigger type and an amount, it looks
     * up the configured PostingRule, builds a balanced journal entry (with a tax line
     * if the rule references one), and submits it.
     */
    JournalEntryDTO postFromEvent(PostingTriggerType triggerEvent, BigDecimal amount, String currency,
                                  String sourceDocType, String sourceDocRef,
                                  Long costCenterId, Long profitCenterId);
}
