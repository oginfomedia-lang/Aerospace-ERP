// PurchaseOrderService.java
package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.POApprovalActionRequest;
import com.aerospace.procurement_service.dto.POApprovalHistoryDTO;
import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;
import com.aerospace.procurement_service.entity.PurchaseRequisition;
import com.aerospace.procurement_service.entity.Quotation;
import com.aerospace.procurement_service.entity.RequestForQuotation;

import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request);
    java.util.List<PurchaseOrderResponse> getAllPurchaseOrders();
    PurchaseOrderResponse updatePOStatus(Long id, com.aerospace.procurement_service.entity.POStatus status);
    void createDraftPOFromStockLow(com.aerospace.procurement_service.event.StockLowEvent event);
    void createDraftPOFromShortage(com.aerospace.procurement_service.event.MaterialShortageEvent event);

    // Updates the matching PO line item's accepted/rejected quantity + NCR reference
    // once inventory-service reports a QA decision against a traceable PO/SKU.
    void applyQaOutcome(com.aerospace.procurement_service.event.QAStatusChangedEvent event);

    // Accumulates received quantity against the matching PO line once a GRN is formed in
    // inventory-service, and auto-transitions the PO to PARTIALLY_RECEIVED/RECEIVED.
    void applyMaterialReceipt(com.aerospace.procurement_service.event.MaterialReceivedEvent event);

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

    // Admin recovery tool: recomputes status/currentApprovalLevel purely from this PO's
    // own live-version approval history, for documents left inconsistent by a bug (fixed
    // this session, but couldn't retroactively repair records it had already corrupted).
    PurchaseOrderResponse resyncApprovalStatus(Long id);

    void createFromAwardedQuotation(PurchaseRequisition pr, RequestForQuotation rfq, Quotation winner);

    // Supplier portal — a supplier's own POs, so they can see what happens after they win an RFQ
    List<PurchaseOrderResponse> getForSupplier(Long supplierId);
}
