// PurchaseOrderService.java
package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.POApprovalActionRequest;
import com.aerospace.procurement_service.dto.POApprovalHistoryDTO;
import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;

import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request);
    java.util.List<PurchaseOrderResponse> getAllPurchaseOrders();
    PurchaseOrderResponse updatePOStatus(Long id, com.aerospace.procurement_service.entity.POStatus status);
    void createDraftPOFromStockLow(com.aerospace.procurement_service.event.StockLowEvent event);

    // Only permitted while status == DRAFT — this is what lets a mistaken SKU/supplier be fixed
    PurchaseOrderResponse updatePurchaseOrder(Long id, PurchaseOrderRequest request);

    // Only permitted while status == DRAFT and the PO was never submitted (no approval history)
    void deletePurchaseOrder(Long id);

    // PUR-BR-001: evaluates the approval matrix and starts (or auto-clears) the approval chain
    PurchaseOrderResponse submitForApproval(Long id);

    // Records an immutable approval at the current pending level; advances or completes the chain
    PurchaseOrderResponse approve(Long id, POApprovalActionRequest request, String approverUsername, List<String> approverRoles);

    // PUR-BR-002/PUR-BR-003: mandatory remarks, resets PO to DRAFT for a full resubmission
    PurchaseOrderResponse reject(Long id, POApprovalActionRequest request, String approverUsername, List<String> approverRoles);

    List<POApprovalHistoryDTO> getApprovalHistory(Long id);
}
