package com.aerospace.qa_service.event.consumer;

import com.aerospace.qa_service.event.QAKafkaTopics;
import com.aerospace.qa_service.event.StockReceivedEvent;
import com.aerospace.qa_service.service.FaiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QAConsumer {

    private final FaiService faiService;

    @KafkaListener(topics = QAKafkaTopics.INVENTORY_STOCK_RECEIVED, groupId = "qa-group")
    public void onStockReceived(StockReceivedEvent event) {
        log.info("QA QUEUE: New batch pending inspection — Part: '{}' (ID: {}), SKU: {}",
                 event.getName(), event.getId(), event.getSerialNumber());

        // Auto-queue a PENDING First Article Inspection the first time this part is
        // ever received — idempotent, never breaks the log-only behavior above on failure.
        try {
            faiService.autoQueueIfFirstArticle(event.getId(), event.getName(), event.getSerialNumber());
        } catch (Exception ex) {
            log.warn("[FAI] Failed to auto-queue first article for partId={}: {}", event.getId(), ex.getMessage());
        }
    }
}
