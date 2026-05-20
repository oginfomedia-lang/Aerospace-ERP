package com.aerospace.procurement_service.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.aerospace.procurement_service.event.StockLowEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {
    private final com.aerospace.procurement_service.service.PurchaseOrderService poService;

    @KafkaListener(topics = "inventory-stock-low", groupId = "procurement-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleStockLowEvent(StockLowEvent event) {
        //1.log it so we can see it working in the terminal
        log.warn("🚨 [PROCUREMENT ALERT] Low stock detected for Part: {} (ID: {}). Current Qty: {} (Reorder Level: {})", 
            event.getPartName(),
            event.getPartId(),
            event.getCurrentQuantity(),
            event.getReorderLevel()
        );

        //2. Automatically create draft PO
        try {
            poService.createDraftPOFromStockLow(event);
        } catch (Exception e) {
            log.error("Failed to automatically create draft PO from stock low event", e);
        }
    }
}
