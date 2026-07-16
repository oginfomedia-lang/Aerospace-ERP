package com.aerospace.production_service.entity;

public enum OperationType {
    PRODUCTION,     // e.g. Cutting, Drilling — reports quantity produced only, no accept/reject decision
    QA_INSPECTION   // a distinct quality-check step — makes the good/rework/scrap decision
}
