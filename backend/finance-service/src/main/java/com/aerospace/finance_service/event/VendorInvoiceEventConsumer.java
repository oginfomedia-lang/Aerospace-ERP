package com.aerospace.finance_service.event;

import com.aerospace.finance_service.entity.PostingTriggerType;
import com.aerospace.finance_service.service.PostingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VendorInvoiceEventConsumer {

    private final PostingEngine postingEngine;

    @KafkaListener(topics = "vendor-invoice-approved", groupId = "finance-service")
    public void onVendorInvoiceApproved(VendorInvoiceApprovedEvent event) {
        log.info("Received VendorInvoiceApprovedEvent for invoice {}", event.getInvoiceNumber());
        try {
            // event.getSupplierId() was previously never used — every auto-posted AP
            // line was untagged and invisible to the AP ledger/aging view. Now tagged.
            postingEngine.postFromEvent(PostingTriggerType.VENDOR_INVOICE_APPROVED,
                    event.getTotalAmount(), event.getCurrency(), "VENDOR_INVOICE", event.getInvoiceNumber(),
                    null, null, event.getSupplierId(), null);
        } catch (Exception ex) {
            log.error("Failed to post journal for VendorInvoiceApprovedEvent invoice={}: {}",
                    event.getInvoiceNumber(), ex.getMessage(), ex);
        }
    }
}
