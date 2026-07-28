package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.RequisitionDTOs.CreatePurchaseRequisitionRequest;
import com.aerospace.procurement_service.dto.RequisitionDTOs.PurchaseRequisitionResponse;
import com.aerospace.procurement_service.event.MaterialShortageEvent;
import com.aerospace.procurement_service.event.StockLowEvent;

import java.util.List;

public interface PurchaseRequisitionService {
    void createFromShortage(MaterialShortageEvent event);
    void createFromStockLow(StockLowEvent event);
    PurchaseRequisitionResponse createManual(CreatePurchaseRequisitionRequest request);
    List<PurchaseRequisitionResponse> getAll();
    PurchaseRequisitionResponse getById(Long id);

    // BP-001 §4.9 — PR approval workflow
    PurchaseRequisitionResponse approve(Long id, String approverUsername);
    PurchaseRequisitionResponse reject(Long id, String approverUsername, String remarks);
}
