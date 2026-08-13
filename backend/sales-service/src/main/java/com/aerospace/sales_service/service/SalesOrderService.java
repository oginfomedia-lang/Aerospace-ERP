package com.aerospace.sales_service.service;

import com.aerospace.sales_service.dto.AtpResultDTO;
import com.aerospace.sales_service.dto.OrderTrackingDTO;
import com.aerospace.sales_service.dto.SalesOrderRequestDTO;
import com.aerospace.sales_service.dto.SalesOrderResponseDTO;

import java.util.List;
import java.util.Set;

public interface SalesOrderService {
    SalesOrderResponseDTO createOrder(SalesOrderRequestDTO dto, String createdBy);

    // DRAFT: edited in place. CONFIRMED or later: the old row is superseded and a new
    // revision (same orderNumber, revisionNumber + 1) is created instead — see impl.
    SalesOrderResponseDTO updateOrder(Long id, SalesOrderRequestDTO dto);

    List<SalesOrderResponseDTO> listOrders(String status);

    // Every revision of a given order number, newest first.
    List<SalesOrderResponseDTO> getHistory(String orderNumber);

    SalesOrderResponseDTO getById(Long id);

    // Approval — thin wrappers around common-lib's already-proven ApprovalEngine (the
    // same engine PurchaseOrderServiceImpl already uses in production today).
    SalesOrderResponseDTO submitForApproval(Long id);
    SalesOrderResponseDTO approve(Long id, String approverUsername, Set<String> approverRoles, String remarks);
    SalesOrderResponseDTO reject(Long id, String approverUsername, Set<String> approverRoles, String remarks);

    // Runs ATP for every line on the order at once, and sets promisedDeliveryDate to
    // the latest (worst-case) earliestDeliveryDate across all lines — a partial
    // shipment isn't "done" until every line is.
    List<AtpResultDTO> evaluateAtp(Long orderId);

    // §18.9: reserves real stock against a CONFIRMED order, batch by batch, so that
    // stock becomes invisible to every other order. Supports partial allocation —
    // stays CONFIRMED if any line came up short, flips to ALLOCATED only once every
    // line got everything it needed.
    SalesOrderResponseDTO allocateOrder(Long orderId);

    // §18.12: "which batch, which shipment" — read-only rollup built entirely from
    // data this service already owns.
    OrderTrackingDTO getTracking(Long orderId);

    SalesOrderResponseDTO releaseAllocation(Long orderId);
}
