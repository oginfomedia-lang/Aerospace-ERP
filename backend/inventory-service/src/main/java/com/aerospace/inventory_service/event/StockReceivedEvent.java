package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// This event did not exist in inventory-service — created to match
// notification-service's StockReceivedEvent fields exactly.
// Published by InventoryProducer.sendStockReceivedEvent()
// Consumed by NotificationConsumer.onStockReceived() on topic: stock-received-events

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReceivedEvent {
    private Long partId;
    private String partName;
    private String batchNumber;
    private int quantityReceived;
    private String supplier;
    private String location;
    private String receivedBy;
}