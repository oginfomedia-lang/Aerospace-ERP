package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLowEvent {
    // Legacy field — renamed below but kept via currentQty alias
    private Long partId;
    private String partName;
    private String skuCode; // the actual reorderable SKU — partName is NOT a valid PO line item code

    // ── Fields required by notification-service ──────────────────────────────
    private String batchNumber;
    private int currentQuantity;   // notification-service reads: event.getCurrentQuantity()
    private int reorderLevel;      // notification-service reads: event.getReorderLevel()
    private String location;

    // Legacy alias — in case any existing code still sets currentQty
    public void setCurrentQty(int qty) { this.currentQuantity = qty; }
    public int getCurrentQty()         { return this.currentQuantity; }
}