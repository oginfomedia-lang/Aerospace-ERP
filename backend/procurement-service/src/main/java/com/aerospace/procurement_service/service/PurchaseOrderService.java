// PurchaseOrderService.java
package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request);
}
