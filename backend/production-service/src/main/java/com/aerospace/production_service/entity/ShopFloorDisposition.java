package com.aerospace.production_service.entity;

public enum ShopFloorDisposition {
    SCRAP,   // permanently out of the flow — ProductionMaterial.quantityWasted incremented
    REWORK   // sent back to redo the same operation — a new attempt row is created
}
