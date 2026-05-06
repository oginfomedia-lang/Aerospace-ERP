package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartCreatedEvent {
    // Legacy fields — kept so existing InventoryConsumer.consumeCreated() still works
    private Long id;
    private String name;
    private String serialNumber;

    // ── New fields required by notification-service ──────────────────────────
    private Long partId;
    private String partName;
    private String batchNumber;
    private int initialQuantity;
    private int reorderLevel;
    private String location;
    private String createdBy;
}