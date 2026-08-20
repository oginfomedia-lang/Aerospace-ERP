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
public class FinishedGoodsReceiptEventConsumer {

    private final PostingEngine postingEngine;

    @KafkaListener(topics = "finished-goods-receipt-events", groupId = "finance-service")
    public void onFinishedGoodsReceipt(FinishedGoodsReceiptEvent event) {
        if (event.getProductionCost() == null) {
            log.warn("FinishedGoodsReceiptEvent for {} has no cost — skipping", event.getReceiptNumber());
            return;
        }
        log.info("Received FinishedGoodsReceiptEvent for receipt {}", event.getReceiptNumber());
        try {
            postingEngine.postFromEvent(PostingTriggerType.FINISHED_GOODS_RECEIPT,
                    event.getProductionCost(), event.getCurrency(), "FINISHED_GOODS_RECEIPT",
                    event.getReceiptNumber(), event.getCostCenterId(), null);
        } catch (Exception ex) {
            log.error("Failed to post journal for FinishedGoodsReceiptEvent ref={}: {}",
                    event.getReceiptNumber(), ex.getMessage(), ex);
        }
    }
}
