package com.aerospace.sales_service.service;

import com.aerospace.sales_service.dto.DispatchResponseDTO;

import java.util.List;

public interface DispatchService {
    // Order must be fully ALLOCATED. Snapshots each line's allocated quantity onto a
    // new Dispatch/DispatchLine set and flips the order to IN_FULFILLMENT.
    DispatchResponseDTO createPickList(Long orderId);

    DispatchResponseDTO markPacked(Long dispatchId, String packedBy);

    // THE moment real stock actually leaves the building (§18.10) — consumes the exact
    // batches recorded at allocation time, marks those allocations SHIPPED, bumps each
    // line's quantityShipped, and publishes SALES_ORDER_DISPATCHED.
    DispatchResponseDTO confirmDispatch(Long dispatchId, String carrierName, String trackingNumber, String dispatchedBy);

    List<DispatchResponseDTO> listByOrder(Long orderId);

    DispatchResponseDTO getById(Long id);
}
