package com.aerospace.sales_service.service.impl;

import com.aerospace.common.approval.ApprovalEngine;
import com.aerospace.common.approval.ApprovalHistory;
import com.aerospace.common.approval.ApprovalOutcome;
import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.sales_service.client.SalesResourceClient;
import com.aerospace.sales_service.client.dto.PartStockInfo;
import com.aerospace.sales_service.client.dto.StockBalanceInfo;
import com.aerospace.sales_service.dto.AtpResultDTO;
import com.aerospace.sales_service.dto.OrderTrackingDTO;
import com.aerospace.sales_service.dto.SalesOrderLineRequestDTO;
import com.aerospace.sales_service.dto.SalesOrderLineResponseDTO;
import com.aerospace.sales_service.dto.SalesOrderRequestDTO;
import com.aerospace.sales_service.dto.SalesOrderResponseDTO;
import com.aerospace.sales_service.entity.AllocationStatus;
import com.aerospace.sales_service.entity.Customer;
import com.aerospace.sales_service.entity.SalesOrder;
import com.aerospace.sales_service.entity.SalesOrderAllocation;
import com.aerospace.sales_service.entity.SalesOrderLine;
import com.aerospace.sales_service.entity.SalesOrderStatus;
import com.aerospace.sales_service.exception.ResourceNotFoundException;
import com.aerospace.sales_service.repository.CustomerRepository;
import com.aerospace.sales_service.repository.DispatchRepository;
import com.aerospace.sales_service.repository.SalesOrderAllocationRepository;
import com.aerospace.sales_service.repository.SalesOrderRepository;
import com.aerospace.sales_service.service.AtpService;
import com.aerospace.sales_service.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesOrderServiceImpl implements SalesOrderService {

    // Same moduleCode concept PurchaseOrderServiceImpl uses — this string is the key
    // ApprovalMatrix rows are looked up by, and must be seeded there before submitting
    // an order does anything more than instantly auto-approve.
    private static final String MODULE = "SALES_ORDER";

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final DocumentSequenceService documentSequenceService;
    private final ApprovalEngine approvalEngine;
    private final AtpService atpService;
    private final SalesResourceClient salesResourceClient;
    private final SalesOrderAllocationRepository allocationRepository;
    private final DispatchRepository dispatchRepository;

    @Override
    @Transactional
    public SalesOrderResponseDTO createOrder(SalesOrderRequestDTO dto, String createdBy) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + dto.getCustomerId()));

        SalesOrder order = SalesOrder.builder()
                .orderNumber(documentSequenceService.generate("SO"))
                .customer(customer)
                .orderType(dto.getOrderType())
                .requestedDeliveryDate(dto.getRequestedDeliveryDate())
                .shippingAddress(dto.getShippingAddress())
                .priority(dto.getPriority())
                .status(SalesOrderStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        List<SalesOrderLine> lines = dto.getLines().stream().map(l -> toLineEntity(l, order)).toList();
        order.setLines(lines);
        order.setTotalAmount(sumLines(lines));

        SalesOrder saved = salesOrderRepository.save(order);
        log.info("Sales order created — orderNumber: {}, customer: {}, total: {}",
                saved.getOrderNumber(), customer.getName(), saved.getTotalAmount());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public SalesOrderResponseDTO updateOrder(Long id, SalesOrderRequestDTO dto) {
        SalesOrder existing = findOrThrow(id);
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + dto.getCustomerId()));

        if (existing.getStatus() == SalesOrderStatus.DRAFT) {
            // Nothing depends on a DRAFT yet — safe to edit in place.
            existing.setCustomer(customer);
            existing.setOrderType(dto.getOrderType());
            existing.setRequestedDeliveryDate(dto.getRequestedDeliveryDate());
            existing.setShippingAddress(dto.getShippingAddress());
            existing.setPriority(dto.getPriority());
            existing.getLines().clear();
            existing.getLines().addAll(dto.getLines().stream().map(l -> toLineEntity(l, existing)).toList());
            existing.setTotalAmount(sumLines(existing.getLines()));
            return toDTO(salesOrderRepository.save(existing));
        }

        // CONFIRMED or later: never mutate — an Allocation/Dispatch may already reference
        // this exact row. Supersede it and create the next revision instead, exactly like
        // RoutingTemplateServiceImpl.update() already does for routing templates.
        existing.setIsCurrentRevision(false);
        salesOrderRepository.save(existing);

        SalesOrder next = SalesOrder.builder()
                .orderNumber(existing.getOrderNumber()) // SAME order number — a revision, not a new order
                .customer(customer)
                .orderType(dto.getOrderType())
                .requestedDeliveryDate(dto.getRequestedDeliveryDate())
                .shippingAddress(dto.getShippingAddress())
                .priority(dto.getPriority())
                .revisionNumber(existing.getRevisionNumber() + 1)
                .supersedesId(existing.getId())
                .isCurrentRevision(true)
                .status(SalesOrderStatus.DRAFT) // a revised order must go through approval again
                .sourceQuotationId(existing.getSourceQuotationId())
                .createdBy(existing.getCreatedBy())
                .build();
        next.setLines(dto.getLines().stream().map(l -> toLineEntity(l, next)).toList());
        next.setTotalAmount(sumLines(next.getLines()));
        SalesOrder saved = salesOrderRepository.save(next);
        log.info("[SALES_ORDER] Revised — orderNumber={}, revision={} (supersedes id {})",
                saved.getOrderNumber(), saved.getRevisionNumber(), existing.getId());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesOrderResponseDTO> listOrders(String status) {
        List<SalesOrder> orders = status == null
                ? salesOrderRepository.findByIsCurrentRevisionTrue()
                : salesOrderRepository.findByStatus(SalesOrderStatus.valueOf(status));
        return orders.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesOrderResponseDTO> getHistory(String orderNumber) {
        return salesOrderRepository.findByOrderNumberOrderByRevisionNumberDesc(orderNumber).stream()
                .map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderResponseDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    // §18.18 ADR-DDS-052: Sales Orders drive production demand, so a bad one is
    // expensive — route it through the same generic, amount-banded ApprovalEngine
    // PurchaseOrderServiceImpl already uses in production. An empty chain means no
    // ApprovalMatrix row qualifies (nothing configured, or the amount is under every
    // band's floor) — the engine's own documented contract is "caller auto-approves".
    @Override
    @Transactional
    public SalesOrderResponseDTO submitForApproval(Long id) {
        SalesOrder order = findOrThrow(id);
        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT order can be submitted for approval (current: " + order.getStatus() + ")");
        }
        List<ApprovalHistory> chain = approvalEngine.submitForApproval(MODULE, order.getId(), order.getTotalAmount(), order.getRevisionNumber());
        if (chain.isEmpty()) {
            order.setStatus(SalesOrderStatus.CONFIRMED);
            log.info("[SALES_ORDER] No approval band configured — auto-confirmed — orderNumber={}", order.getOrderNumber());
        } else {
            order.setStatus(SalesOrderStatus.PENDING_APPROVAL);
            log.info("[SALES_ORDER] Submitted for approval — orderNumber={}, amount={}", order.getOrderNumber(), order.getTotalAmount());
        }
        return toDTO(salesOrderRepository.save(order));
    }

    @Override
    @Transactional
    public SalesOrderResponseDTO approve(Long id, String approverUsername, Set<String> approverRoles, String remarks) {
        SalesOrder order = findOrThrow(id);
        if (order.getStatus() != SalesOrderStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Order is not awaiting approval (current: " + order.getStatus() + ")");
        }
        ApprovalOutcome outcome = approvalEngine.approve(MODULE, order.getId(), approverUsername, approverRoles, remarks);
        if (outcome.status() == ApprovalOutcome.Status.FULLY_APPROVED) {
            order.setStatus(SalesOrderStatus.CONFIRMED); // every approval level cleared — a real commitment now
            log.info("[SALES_ORDER] Fully approved and CONFIRMED — orderNumber={}", order.getOrderNumber());
        }
        // else: still PENDING_APPROVAL, waiting on the next level — status intentionally unchanged
        return toDTO(salesOrderRepository.save(order));
    }

    @Override
    @Transactional
    public SalesOrderResponseDTO reject(Long id, String approverUsername, Set<String> approverRoles, String remarks) {
        SalesOrder order = findOrThrow(id);
        if (order.getStatus() != SalesOrderStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Order is not awaiting approval (current: " + order.getStatus() + ")");
        }
        approvalEngine.reject(MODULE, order.getId(), approverUsername, approverRoles, remarks);
        order.setStatus(SalesOrderStatus.REJECTED);
        log.info("[SALES_ORDER] Rejected — orderNumber={}", order.getOrderNumber());
        return toDTO(salesOrderRepository.save(order));
    }

    @Override
    @Transactional
    public List<AtpResultDTO> evaluateAtp(Long orderId) {
        SalesOrder order = findOrThrow(orderId);
        List<AtpResultDTO> results = order.getLines().stream()
                .map(l -> atpService.evaluate(l.getId(), l.getPartId(), l.getPartNumber(), l.getQuantity()))
                .toList();

        // The order's overall promise is only as good as its worst line — a partial
        // shipment isn't "done" until every line is, so promisedDeliveryDate takes the
        // LATEST earliestDeliveryDate across all lines, not the earliest.
        results.stream()
                .map(AtpResultDTO::getEarliestDeliveryDate)
                .max(java.time.LocalDate::compareTo)
                .ifPresent(order::setPromisedDeliveryDate);
        salesOrderRepository.save(order);

        return results;
    }

    // §18.9: reserve real stock, batch by batch, against every line still short of its
    // ordered quantity. inventory-service's reserveStock needs an EXACT batch (it has
    // no "just give me N units, you pick" endpoint), so the batch-picking happens here:
    // take APPROVED batches oldest-expiry-first (FEFO — the same principle this app's
    // expiry tracking exists for elsewhere), draining each one before moving to the next,
    // until either the line is fully covered or stock runs out.
    @Override
    @Transactional
    public SalesOrderResponseDTO allocateOrder(Long orderId) {
        SalesOrder order = findOrThrow(orderId);
        if (order.getStatus() != SalesOrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only a CONFIRMED order can be allocated (current: " + order.getStatus() + ")");
        }

        // Skipping a failed batch (below) must never look like silent, unqualified
        // success — if literally nothing could be reserved this call, say so honestly
        // instead of returning a "successful" response where nothing actually changed.
        int totalReservedThisCall = 0;

        for (SalesOrderLine line : order.getLines()) {
            int remaining = line.getQuantity() - line.getQuantityAllocated();
            if (remaining <= 0) continue;

            PartStockInfo stock = salesResourceClient.getPartStock(line.getPartId());
            List<StockBalanceInfo> approvedBatches = stock.getBalances() == null ? List.of()
                    : stock.getBalances().stream()
                        .filter(b -> "APPROVED".equalsIgnoreCase(b.getStatus()) && b.getQuantity() != null && b.getQuantity() > 0)
                        .sorted(Comparator.comparing(StockBalanceInfo::getExpiryDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();

            for (StockBalanceInfo batch : approvedBatches) {
                if (remaining <= 0) break;
                int take = Math.min(remaining, batch.getQuantity());

                // Can fail (a concurrent reservation beat us to this batch, or the
                // snapshot we read a moment ago is already stale) — that's allowed to
                // happen; a partial allocation across lines is fine (§18.9 "Partial
                // Allocation"). This whole method is @Transactional, so an uncaught
                // exception here would roll back EVERY batch already reserved earlier
                // in this same call — including on inventory-service, whose reservation
                // doesn't share this transaction and wouldn't roll back with it. Catching
                // and skipping keeps this line's shortfall local instead of losing
                // already-successful work.
                try {
                    salesResourceClient.reserveStock(line.getPartId(), batch.getBatchNumber(), take);
                } catch (Exception ex) {
                    log.warn("[ALLOCATION] Failed to reserve {} units of batch {} for part {} — skipping this batch: {}",
                            take, batch.getBatchNumber(), line.getPartId(), ex.getMessage());
                    continue;
                }

                allocationRepository.save(SalesOrderAllocation.builder()
                        .salesOrderLine(line).partId(line.getPartId())
                        .batchNumber(batch.getBatchNumber()).warehouseLocation(batch.getLocation())
                        .quantity(take).status(AllocationStatus.ACTIVE)
                        .build());

                line.setQuantityAllocated(line.getQuantityAllocated() + take);
                remaining -= take;
                totalReservedThisCall += take;
            }
        }

        if (totalReservedThisCall == 0) {
            throw new IllegalStateException(
                    "No APPROVED stock is currently available to reserve for any line on this order — check inventory for the part(s) involved.");
        }

        boolean fullyAllocated = order.getLines().stream()
                .allMatch(l -> l.getQuantityAllocated() >= l.getQuantity());
        order.setStatus(fullyAllocated ? SalesOrderStatus.ALLOCATED : SalesOrderStatus.CONFIRMED);
        log.info("[ALLOCATION] Order {} — fullyAllocated={}", order.getOrderNumber(), fullyAllocated);
        return toDTO(salesOrderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingDTO getTracking(Long orderId) {
        SalesOrder order = findOrThrow(orderId);

        List<OrderTrackingDTO.DispatchSummary> dispatches = dispatchRepository.findBySalesOrder_Id(orderId).stream()
                .map(d -> OrderTrackingDTO.DispatchSummary.builder()
                        .dispatchNumber(d.getDispatchNumber())
                        .status(d.getStatus().name())
                        .carrierName(d.getCarrierName())
                        .trackingNumber(d.getTrackingNumber())
                        .dispatchedAt(d.getDispatchedAt())
                        .build())
                .toList();

        List<OrderTrackingDTO.AllocationSummary> allocations = allocationRepository.findBySalesOrderLine_SalesOrder_Id(orderId).stream()
                .map(a -> OrderTrackingDTO.AllocationSummary.builder()
                        .partNumber(a.getSalesOrderLine().getPartNumber())
                        .batchNumber(a.getBatchNumber())
                        .quantity(a.getQuantity())
                        .status(a.getStatus().name())
                        .build())
                .toList();

        return OrderTrackingDTO.builder()
                .salesOrderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .dispatches(dispatches)
                .allocations(allocations)
                .build();
    }

    // Same recompute-server-side-never-trust-the-client math as
    // QuotationServiceImpl.toLineEntity — duplicated rather than shared, since it's
    // three lines and the two entities (SalesOrderLine vs QuotationLine) are genuinely
    // different types; not worth a premature shared helper for this little logic.
    private SalesOrderLine toLineEntity(SalesOrderLineRequestDTO d, SalesOrder order) {
        BigDecimal discount = d.getDiscountPercent() != null ? d.getDiscountPercent() : BigDecimal.ZERO;
        BigDecimal tax = d.getTaxPercent() != null ? d.getTaxPercent() : BigDecimal.ZERO;
        BigDecimal gross = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
        BigDecimal afterDiscount = gross.multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100))));
        BigDecimal lineTotal = afterDiscount.multiply(BigDecimal.ONE.add(tax.divide(BigDecimal.valueOf(100))))
                .setScale(2, RoundingMode.HALF_UP);
        return SalesOrderLine.builder()
                .salesOrder(order).partId(d.getPartId()).partNumber(d.getPartNumber()).partName(d.getPartName())
                .quantity(d.getQuantity()).unitPrice(d.getUnitPrice()).discountPercent(discount).taxPercent(tax)
                .lineTotal(lineTotal).quantityAllocated(0).quantityShipped(0)
                .build();
    }

    private BigDecimal sumLines(List<SalesOrderLine> lines) {
        return lines.stream().map(SalesOrderLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SalesOrder findOrThrow(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found with ID: " + id));
    }

    private SalesOrderResponseDTO toDTO(SalesOrder o) {
        List<SalesOrderLineResponseDTO> lineDTOs = o.getLines().stream()
                .map(l -> SalesOrderLineResponseDTO.builder()
                        .id(l.getId()).partId(l.getPartId()).partNumber(l.getPartNumber()).partName(l.getPartName())
                        .quantity(l.getQuantity()).unitPrice(l.getUnitPrice()).discountPercent(l.getDiscountPercent())
                        .taxPercent(l.getTaxPercent()).lineTotal(l.getLineTotal())
                        .quantityAllocated(l.getQuantityAllocated()).quantityShipped(l.getQuantityShipped())
                        .build())
                .toList();
        return SalesOrderResponseDTO.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .customerId(o.getCustomer().getId())
                .customerName(o.getCustomer().getName())
                .orderType(o.getOrderType())
                .requestedDeliveryDate(o.getRequestedDeliveryDate())
                .promisedDeliveryDate(o.getPromisedDeliveryDate())
                .shippingAddress(o.getShippingAddress())
                .priority(o.getPriority())
                .status(o.getStatus())
                .totalAmount(o.getTotalAmount())
                .sourceQuotationId(o.getSourceQuotationId())
                .revisionNumber(o.getRevisionNumber())
                .supersedesId(o.getSupersedesId())
                .isCurrentRevision(o.getIsCurrentRevision())
                .lines(lineDTOs)
                .createdBy(o.getCreatedBy())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}