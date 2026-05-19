package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;
import com.aerospace.procurement_service.entity.POLineItem;
import com.aerospace.procurement_service.entity.POStatus;
import com.aerospace.procurement_service.entity.PurchaseOrder;
import com.aerospace.procurement_service.entity.Supplier;
import com.aerospace.procurement_service.mapper.PurchaseOrderMapper;
import com.aerospace.procurement_service.repository.PurchaseOrderRepository;
import com.aerospace.procurement_service.service.PurchaseOrderService;
import com.aerospace.procurement_service.service.SupplierService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final SupplierService supplierService;
    private final PurchaseOrderMapper poMapper;
    private final com.aerospace.procurement_service.event.producer.ProcurementEventPublisher eventPublisher;

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        // 1. Fetch the supplier. (If not found, it throws our custom exception)
        Supplier supplier = supplierService.getSupplierById(request.getSupplierId());

        // Aerospace Business Rule: Is the supplier certified?
        if (!"CERTIFIED".equalsIgnoreCase(supplier.getCertificationStatus())) {
            throw new IllegalStateException("Cannot create PO for an uncertified supplier!");
        }

        // 2. Map the request to an Entity
        PurchaseOrder po = poMapper.toEntity(request);

        // 3. Set server-controlled data (Things the user shouldn't send)
        po.setSupplier(supplier);
        po.setOrderDate(LocalDateTime.now());
        po.setStatus(POStatus.DRAFT);
        po.setPoNumber("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // 4. Calculate total amount by looping through line items
        double totalAmount = 0.0;
        if (po.getItems() != null) {
            for (POLineItem item : po.getItems()) {
                // The POLineItem entity has a @PrePersist method to calculate its own total,
                // but we need the sum of all items for the main PO document.
                double lineTotal = item.getQuantity() * item.getUnitPrice();
                item.setTotalPrice(lineTotal); // Ensure it's set
                totalAmount += lineTotal;
            }
        }
        po.setTotalAmount(totalAmount);

        // 5. Save to database
        PurchaseOrder savedPo = poRepository.save(po);

        // 6. Convert back to a response DTO for the frontend
        return poMapper.toDTO(savedPo);
    }

    @Override
    public java.util.List<PurchaseOrderResponse> getAllPurchaseOrders() {
        return poRepository.findAll().stream()
                .map(poMapper::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    @Override
    public PurchaseOrderResponse updatePOStatus(Long id, com.aerospace.procurement_service.entity.POStatus status) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new com.aerospace.procurement_service.exception.ResourceNotFoundException("PO not found with id " + id));
        po.setStatus(status);
        PurchaseOrder saved = poRepository.save(po);

        if (status == com.aerospace.procurement_service.entity.POStatus.SENT_TO_VENDOR) {
            com.aerospace.procurement_service.event.PurchaseOrderCreatedEvent event = com.aerospace.procurement_service.event.PurchaseOrderCreatedEvent.builder()
                    .purchaseOrderId(saved.getId())
                    .poNumber(saved.getPoNumber())
                    .supplierName(saved.getSupplier().getName())
                    .totalAmount(saved.getTotalAmount())
                    .currency(saved.getCurrency())
                    .expectedDeliveryDate(saved.getDeliveryDate())
                    .build();
            eventPublisher.publishPOCreatedEvent(event);
        }

        return poMapper.toDTO(saved);
    }

    @Transactional
    @Override
    public void createDraftPOFromStockLow(com.aerospace.procurement_service.event.StockLowEvent event) {
        java.util.List<Supplier> suppliers = supplierService.getAllSuppliers();
        Supplier defaultSupplier = suppliers.stream()
                .filter(s -> "CERTIFIED".equalsIgnoreCase(s.getCertificationStatus()))
                .findFirst()
                .orElse(null);

        if (defaultSupplier == null) {
            log.warn("Cannot create draft PO automatically because no certified suppliers exist!");
            return;
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(defaultSupplier);
        po.setOrderDate(LocalDateTime.now());
        po.setDeliveryDate(LocalDateTime.now().plusDays(7));
        po.setStatus(POStatus.DRAFT);
        po.setPoNumber("PO-AUTO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        po.setCurrency("USD");
        po.setRemarks("Auto-drafted due to low stock alert for Part: " + event.getPartName());

        POLineItem item = new POLineItem();
        item.setPurchaseOrder(po);
        item.setSkuCode(event.getPartName());
        item.setQuantity(event.getReorderLevel() * 2);
        item.setUnitPrice(15.00);
        item.setTotalPrice(item.getQuantity() * item.getUnitPrice());
        item.setUom("PCS");

        po.setItems(java.util.List.of(item));
        po.setTotalAmount(item.getTotalPrice());

        poRepository.save(po);
        log.info("📢 [PROCUREMENT AUTO] Successfully created draft PO: {} for Part: {}", po.getPoNumber(), event.getPartName());
    }
}
