package com.aerospace.finance_service.event;

import com.aerospace.finance_service.entity.PostingTriggerType;
import com.aerospace.finance_service.service.PostingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryAdjustmentEventConsumer {

    private final PostingEngine postingEngine;

    @KafkaListener(topics = "inventory-adjustment-events", groupId = "finance-service")
    public void onInventoryAdjustment(InventoryAdjustmentEvent event) {
        if (event.getAdjustmentAmount() == null) {
            log.warn("InventoryAdjustmentEvent for {} has no amount — skipping", event.getAdjustmentNumber());
            return;
        }
        // Use absolute value for the amount — PostingRule determines debit/credit direction
        BigDecimal amount = event.getAdjustmentAmount().abs();
        log.info("Received InventoryAdjustmentEvent for adjustment {}", event.getAdjustmentNumber());
        try {
            postingEngine.postFromEvent(PostingTriggerType.INVENTORY_ADJUSTMENT,
                    amount, event.getCurrency(), "INVENTORY_ADJUSTMENT",
                    event.getAdjustmentNumber(), null, null);
        } catch (Exception ex) {
            log.error("Failed to post journal for InventoryAdjustmentEvent adjustment={}: {}",
                    event.getAdjustmentNumber(), ex.getMessage(), ex);
        }
    }
}
