package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.SupplierRequest;
import com.aerospace.procurement_service.entity.Supplier;
import com.aerospace.procurement_service.exception.DuplicateResourceException;
import com.aerospace.procurement_service.exception.ResourceNotFoundException;
import com.aerospace.procurement_service.mapper.SupplierMapper;
import com.aerospace.procurement_service.repository.SupplierRepository;
import com.aerospace.procurement_service.service.SupplierService;
import com.aerospace.procurement_service.event.producer.ProcurementEventPublisher;
import com.aerospace.procurement_service.entity.SupplierConfirmationStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final ProcurementEventPublisher eventPublisher;

    public Supplier createSupplier(SupplierRequest request) {
        if (supplierRepository.existsByVendorCode(request.getVendorCode())) {
            throw new DuplicateResourceException("Supplier with code " + request.getVendorCode() + " already exists!");
        }

        Supplier supplier = supplierMapper.toEntity(request);
        Supplier saved = supplierRepository.save(supplier);

        // Tells QA a new supplier is sitting in their queue.
        eventPublisher.publishSupplierRegistered(
                com.aerospace.procurement_service.event.SupplierRegisteredEvent.builder()
                        .supplierId(saved.getId())
                        .supplierName(saved.getName())
                        .vendorCode(saved.getVendorCode())
                        .registeredBy(saved.getCreatedBy())
                        .build());

        return saved;
    }

    @Override
    public Supplier updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = getSupplierById(id);

        if (!supplier.getVendorCode().equals(request.getVendorCode())
                && supplierRepository.existsByVendorCode(request.getVendorCode())) {
            throw new DuplicateResourceException("Supplier with code " + request.getVendorCode() + " already exists!");
        }

        supplierMapper.updateEntity(supplier, request);

        // A rejected supplier that gets edited goes back into the QA queue instead
        // of staying rejected forever with no way back in.
        if (supplier.getConfirmationStatus() == SupplierConfirmationStatus.REJECTED) {
            supplier.setConfirmationStatus(SupplierConfirmationStatus.PENDING);
            supplier.setConfirmationRemarks(null);
            supplier.setConfirmedBy(null);
            supplier.setConfirmedAt(null);
        }

        Supplier saved = supplierRepository.save(supplier);
        eventPublisher.publishSupplierUpdated(
                com.aerospace.procurement_service.event.SupplierUpdatedEvent.builder()
                        .supplierId(saved.getId())
                        .supplierName(saved.getName())
                        .vendorCode(saved.getVendorCode())
                        .active(saved.isActive())
                        .updatedBy(saved.getUpdatedBy())
                        .build());
        return saved;
    }

    @Override
    public Supplier confirmSupplier(Long id, String remarks, String actor) {
        Supplier supplier = getSupplierById(id);
        if (supplier.getConfirmationStatus() == SupplierConfirmationStatus.CONFIRMED) {
            throw new IllegalStateException("Supplier is already confirmed");
        }
        supplier.setConfirmationStatus(SupplierConfirmationStatus.CONFIRMED);
        supplier.setConfirmationRemarks(remarks);
        supplier.setConfirmedBy(actor);
        supplier.setConfirmedAt(java.time.LocalDateTime.now());
        Supplier saved = supplierRepository.save(supplier);

        eventPublisher.publishSupplierConfirmationDecided(
                com.aerospace.procurement_service.event.SupplierConfirmationDecidedEvent.builder()
                        .supplierId(saved.getId())
                        .supplierName(saved.getName())
                        .status("CONFIRMED")
                        .decidedBy(actor)
                        .remarks(remarks)
                        .registeredBy(saved.getCreatedBy())
                        .build());
        return saved;
    }

    @Override
    public Supplier rejectSupplier(Long id, String remarks, String actor) {
        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException("Rejection remarks are mandatory");
        }
        Supplier supplier = getSupplierById(id);
        supplier.setConfirmationStatus(SupplierConfirmationStatus.REJECTED);
        supplier.setConfirmationRemarks(remarks);
        supplier.setConfirmedBy(actor);
        supplier.setConfirmedAt(java.time.LocalDateTime.now());
        Supplier saved = supplierRepository.save(supplier);

        eventPublisher.publishSupplierConfirmationDecided(
                com.aerospace.procurement_service.event.SupplierConfirmationDecidedEvent.builder()
                        .supplierId(saved.getId())
                        .supplierName(saved.getName())
                        .status("REJECTED")
                        .decidedBy(actor)
                        .remarks(remarks)
                        .registeredBy(saved.getCreatedBy())
                        .build());
        return saved;
    }
    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Override
    public Supplier setActive(Long id, boolean active) {
        Supplier supplier = getSupplierById(id);
        supplier.setActive(active);
        Supplier saved = supplierRepository.save(supplier);
        eventPublisher.publishSupplierUpdated(
                com.aerospace.procurement_service.event.SupplierUpdatedEvent.builder()
                        .supplierId(saved.getId())
                        .supplierName(saved.getName())
                        .vendorCode(saved.getVendorCode())
                        .active(saved.isActive())
                        .updatedBy(saved.getUpdatedBy())
                        .build());
        return saved;
    }
}
