package com.aerospace.procurement_service.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.aerospace.procurement_service.event.StockLowEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory-stock-low", groupId = "procurement-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleStockLowEvent(StockLowEvent event) {
        //1.log it so we can see it working in the terminal
        log.warn("🚨 [PROCUREMENT ALERT] Low stock detected for Part: {} (ID: {}). Current Qty: {} (Reorder Level: {})", 
            event.getPartName(),
            event.getPartId(),
            event.getCurrentQuantity(),
            event.getReorderLevel()
        );

        //2.todo: call purchaseorderservice.createDraftPO(event.getPartId())
    }
}
