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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final SupplierService supplierService;
    private final PurchaseOrderMapper poMapper;

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
}
