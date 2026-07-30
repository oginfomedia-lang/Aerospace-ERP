package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Published once per line item when a GRN is formed from a Material Receipt (see
// MaterialReceiptServiceImpl.formGrn()). Consumed by procurement-service to accumulate
// received quantity against the originating PO line and auto-update PO status
// (PARTIALLY_RECEIVED / RECEIVED) — see PurchaseOrderServiceImpl.applyMaterialReceipt().
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialReceivedEvent {
    private String poNumber;
    private String skuCode;
    private int quantityReceived;
    private String grnNumber;
}
