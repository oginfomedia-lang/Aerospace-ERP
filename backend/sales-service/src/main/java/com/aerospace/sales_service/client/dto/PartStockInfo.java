package com.aerospace.sales_service.client.dto;

import lombok.Data;
import java.util.List;

// Local mirror of inventory-service's PartResponseDTO — only the fields ATP/Allocation need.
@Data
public class PartStockInfo {
    private Long id;
    private String name;
    private String serialNumber; // inventory-service's field name for SKU/part number
    private Integer quantity;    // APPROVED-only, usable stock (collapsed summary)
    private Integer pendingQaQuantity;

    // Set on GRN-receipt responses (inward-from-grn) — the system-generated batch this
    // receipt landed in, and its resulting status (PENDING_QA or APPROVED). RMA's
    // "receive return" step reads these back to remember which batch to inspect later.
    private String batchNumber;
    private String status;

    // Per-batch breakdown — Allocation needs this to pick WHICH batch(es) to reserve
    // from (inventory-service's reserveStock requires a specific batch, it doesn't
    // auto-pick one), since a part's APPROVED stock can be spread across several batches.
    private List<StockBalanceInfo> balances;
}
