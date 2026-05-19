// PurchaseOrderService.java
package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request);
    java.util.List<PurchaseOrderResponse> getAllPurchaseOrders();
    PurchaseOrderResponse updatePOStatus(Long id, com.aerospace.procurement_service.entity.POStatus status);
    void createDraftPOFromStockLow(com.aerospace.procurement_service.event.StockLowEvent event);
}
