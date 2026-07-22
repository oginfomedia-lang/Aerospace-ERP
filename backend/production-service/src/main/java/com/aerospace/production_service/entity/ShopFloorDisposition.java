package com.aerospace.production_service.entity;

public enum ShopFloorDisposition {
    SCRAP,   // permanently out of the flow — ProductionMaterial.quantityWasted incremented
    REWORK,  // sent back to redo the same operation — a new attempt row is created
    // A finished-goods reject recorded at "Send to QA" (order-level Rejected Qty) — not
    // tied to any one BOM line, so no quantityWasted/rework side effect applies. Purely
    // a traceability record.
    FINISHED_GOODS_REJECT
}
