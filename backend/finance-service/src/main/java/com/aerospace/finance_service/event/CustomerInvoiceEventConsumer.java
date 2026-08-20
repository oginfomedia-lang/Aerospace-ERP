package com.aerospace.finance_service.event;

import com.aerospace.finance_service.entity.PostingTriggerType;
import com.aerospace.finance_service.service.PostingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens for customer invoice events published by the Sales service and
 * auto-posts the AR journal entry via the Posting Engine:
 *   Dr Accounts Receivable (1200)   — the customer owes us
 *   Cr Revenue              (4000)   — revenue recognised
 *
 * event.getCustomerId() is resolved against the local CustomerReference mirror and
 * tags the AR (debit) line, so the AR aging/ledger views can group by customer —
 * counterpartyRef is auto-derived from it, not read from the deprecated customerRef.
 *
 * Note: as of this writing nothing in the backend actually publishes to topic
 * "customer-invoice-created" yet (sales-service has no invoice concept today) — this
 * consumer is correct and ready, just not yet exercised by real traffic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerInvoiceEventConsumer {

    private final PostingEngine postingEngine;

    @KafkaListener(topics = "customer-invoice-created", groupId = "finance-service")
    public void onCustomerInvoiceCreated(CustomerInvoiceCreatedEvent event) {
        if (event.getTotalAmount() == null || event.getTotalAmount().signum() == 0) {
            log.warn("CustomerInvoiceCreatedEvent for {} has no amount — skipping AR posting",
                    event.getInvoiceNumber());
            return;
        }
        log.info("Received CustomerInvoiceCreatedEvent for invoice {}", event.getInvoiceNumber());
        try {
            postingEngine.postFromEvent(
                    PostingTriggerType.CUSTOMER_INVOICE,
                    event.getTotalAmount(),
                    event.getCurrency(),
                    "CUSTOMER_INVOICE",
                    event.getInvoiceNumber(),
                    null, null, null, event.getCustomerId());
        } catch (Exception ex) {
            log.error("Failed to post AR journal for CustomerInvoiceCreatedEvent invoice={}: {}",
                    event.getInvoiceNumber(), ex.getMessage(), ex);
        }
    }
}
