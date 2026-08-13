package com.aerospace.sales_service.service.impl;

import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.sales_service.client.SalesResourceClient;
import com.aerospace.sales_service.dto.DispatchLineResponseDTO;
import com.aerospace.sales_service.dto.DispatchResponseDTO;
import com.aerospace.sales_service.entity.AllocationStatus;
import com.aerospace.sales_service.entity.Dispatch;
import com.aerospace.sales_service.entity.DispatchLine;
import com.aerospace.sales_service.entity.DispatchStatus;
import com.aerospace.sales_service.entity.SalesOrder;
import com.aerospace.sales_service.entity.SalesOrderAllocation;
import com.aerospace.sales_service.entity.SalesOrderStatus;
import com.aerospace.sales_service.event.producer.SalesProducer;
import com.aerospace.sales_service.exception.ResourceNotFoundException;
import com.aerospace.sales_service.repository.DispatchRepository;
import com.aerospace.sales_service.repository.SalesOrderAllocationRepository;
import com.aerospace.sales_service.repository.SalesOrderRepository;
import com.aerospace.sales_service.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchServiceImpl implements DispatchService {

    private final DispatchRepository dispatchRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderAllocationRepository allocationRepository;
    private final DocumentSequenceService documentSequenceService;
    private final SalesResourceClient salesResourceClient;
    private final SalesProducer salesProducer;

    // A pick list tells someone what to physically go grab off the shelves — one line
    // per SalesOrderLine, for whatever quantity was actually allocated to it.
    @Override
    @Transactional
    public DispatchResponseDTO createPickList(Long orderId) {
        SalesOrder order = findOrderOrThrow(orderId);
        if (order.getStatus() != SalesOrderStatus.ALLOCATED) {
            throw new IllegalStateException(
                    "Order must be fully ALLOCATED before a pick list can be created (current: " + order.getStatus() + ")");
        }

        Dispatch dispatch = Dispatch.builder()
                .dispatchNumber(documentSequenceService.generate("DISP"))
                .salesOrder(order)
                .status(DispatchStatus.PENDING)
                .build();
        List<DispatchLine> lines = order.getLines().stream()
                .filter(l -> l.getQuantityAllocated() != null && l.getQuantityAllocated() > 0)
                .map(l -> DispatchLine.builder()
                        .dispatch(dispatch).salesOrderLineId(l.getId())
                        .partId(l.getPartId()).partNumber(l.getPartNumber())
                        .quantity(l.getQuantityAllocated())
                        .build())
                .toList();
        dispatch.setLines(lines);

        order.setStatus(SalesOrderStatus.IN_FULFILLMENT);
        salesOrderRepository.save(order);

        Dispatch saved = dispatchRepository.save(dispatch);
        log.info("[DISPATCH] Pick list created — dispatchNumber={}, orderNumber={}",
                saved.getDispatchNumber(), order.getOrderNumber());
        return toDTO(saved);
    }

    // Just records that a human checked the box contents match the order before
    // sealing it — no inventory or order-status side effects yet.
    @Override
    @Transactional
    public DispatchResponseDTO markPacked(Long dispatchId, String packedBy) {
        Dispatch dispatch = findOrThrow(dispatchId);
        if (dispatch.getStatus() != DispatchStatus.PENDING) {
            throw new IllegalStateException("Dispatch must be PENDING before it can be packed (current: " + dispatch.getStatus() + ")");
        }
        dispatch.setStatus(DispatchStatus.PACKED);
        dispatch.setPackedBy(packedBy);
        dispatch.setPackedAt(LocalDateTime.now());
        return toDTO(dispatchRepository.save(dispatch));
    }

    @Override
    @Transactional
    public DispatchResponseDTO confirmDispatch(Long dispatchId, String carrierName, String trackingNumber, String dispatchedBy) {
        Dispatch dispatch = findOrThrow(dispatchId);
        if (dispatch.getStatus() != DispatchStatus.PACKED) {
            throw new IllegalStateException("Dispatch must be PACKED before it can be confirmed (current: " + dispatch.getStatus() + ")");
        }

        SalesOrder order = dispatch.getSalesOrder();
        for (DispatchLine line : dispatch.getLines()) {
            // Consume the EXACT batches recorded at allocation time — not a fresh
            // FIFO/FEFO pick — so what actually leaves the building matches what was
            // promised/reserved for this specific order.
            List<SalesOrderAllocation> activeAllocations =
                    allocationRepository.findBySalesOrderLineIdAndStatus(line.getSalesOrderLineId(), AllocationStatus.ACTIVE);
            for (SalesOrderAllocation allocation : activeAllocations) {
                salesResourceClient.consumeStock(allocation.getPartId(), allocation.getBatchNumber(), allocation.getQuantity());
                allocation.setStatus(AllocationStatus.SHIPPED);
                allocationRepository.save(allocation);
            }

            order.getLines().stream()
                    .filter(l -> l.getId().equals(line.getSalesOrderLineId()))
                    .findFirst()
                    .ifPresent(soLine -> soLine.setQuantityShipped(soLine.getQuantityShipped() + line.getQuantity()));
        }

        dispatch.setStatus(DispatchStatus.DISPATCHED);
        dispatch.setCarrierName(carrierName);
        dispatch.setTrackingNumber(trackingNumber);
        dispatch.setDispatchedBy(dispatchedBy);
        dispatch.setDispatchedAt(LocalDateTime.now());
        Dispatch saved = dispatchRepository.save(dispatch);

        boolean fullyShipped = order.getLines().stream()
                .allMatch(l -> l.getQuantityShipped() >= l.getQuantity());
        order.setStatus(fullyShipped ? SalesOrderStatus.DISPATCHED : SalesOrderStatus.IN_FULFILLMENT);
        salesOrderRepository.save(order);

        salesProducer.sendSalesOrderDispatched(order.getId(), order.getOrderNumber(),
                saved.getDispatchNumber(), saved.getTrackingNumber(), order.getCustomer().getId());
        log.info("[DISPATCH] Confirmed — dispatchNumber={}, orderNumber={}, tracking={}",
                saved.getDispatchNumber(), order.getOrderNumber(), trackingNumber);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchResponseDTO> listByOrder(Long orderId) {
        return dispatchRepository.findBySalesOrder_Id(orderId).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchResponseDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    private SalesOrder findOrderOrThrow(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found with ID: " + id));
    }

    private Dispatch findOrThrow(Long id) {
        return dispatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch not found with ID: " + id));
    }

    private DispatchResponseDTO toDTO(Dispatch d) {
        List<DispatchLineResponseDTO> lineDTOs = d.getLines().stream()
                .map(l -> DispatchLineResponseDTO.builder()
                        .id(l.getId()).salesOrderLineId(l.getSalesOrderLineId())
                        .partId(l.getPartId()).partNumber(l.getPartNumber()).quantity(l.getQuantity())
                        .build())
                .toList();
        return DispatchResponseDTO.builder()
                .id(d.getId())
                .dispatchNumber(d.getDispatchNumber())
                .salesOrderId(d.getSalesOrder().getId())
                .orderNumber(d.getSalesOrder().getOrderNumber())
                .status(d.getStatus())
                .carrierName(d.getCarrierName())
                .trackingNumber(d.getTrackingNumber())
                .packedBy(d.getPackedBy())
                .dispatchedBy(d.getDispatchedBy())
                .packedAt(d.getPackedAt())
                .dispatchedAt(d.getDispatchedAt())
                .lines(lineDTOs)
                .createdAt(d.getCreatedAt())
                .build();
    }
}
