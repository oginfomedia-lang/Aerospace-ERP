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
public class SalesDispatchEventConsumer {

    private final PostingEngine postingEngine;

    @KafkaListener(topics = "sales-order-dispatched-events", groupId = "finance-service")
    public void onSalesOrderDispatched(SalesOrderDispatchedEvent event) {
        if (event.getTotalAmount() == null) {
            log.warn("SalesOrderDispatchedEvent for {} has no totalAmount — skipping revenue posting", event.getDispatchNumber());
            return;
        }
        log.info("Received SalesOrderDispatchedEvent for dispatch {}", event.getDispatchNumber());
        try {
            postingEngine.postFromEvent(PostingTriggerType.SALES_DISPATCH_CONFIRMED,
                    event.getTotalAmount(), event.getCurrency(), "SALES_DISPATCH", event.getDispatchNumber(), null, null);
        } catch (Exception ex) {
            log.error("Failed to post journal for SalesOrderDispatchedEvent dispatch={}: {}",
                    event.getDispatchNumber(), ex.getMessage(), ex);
        }
    }
}
