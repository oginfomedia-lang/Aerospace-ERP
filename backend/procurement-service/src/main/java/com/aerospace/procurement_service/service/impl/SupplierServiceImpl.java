package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.SupplierRequest;
import com.aerospace.procurement_service.entity.Supplier;
import com.aerospace.procurement_service.exception.DuplicateResourceException;
import com.aerospace.procurement_service.exception.ResourceNotFoundException;
import com.aerospace.procurement_service.mapper.SupplierMapper;
import com.aerospace.procurement_service.repository.SupplierRepository;
import com.aerospace.procurement_service.service.SupplierService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public Supplier createSupplier(SupplierRequest request) {
        // Business Rule: We cannot have two suppliers with the same Vendor Code
        if (supplierRepository.existsByVendorCode(request.getVendorCode())) {
            throw new DuplicateResourceException("Supplier with code " + request.getVendorCode() + " already exists!");
        }

        // Convert the Request DTO into a Database Entity
        Supplier supplier = supplierMapper.toEntity(request);
        
        // Save to database
        return supplierRepository.save(supplier);
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Override
    public Supplier updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = getSupplierById(id);

        // Vendor code must stay unique, but changing it to itself is fine
        if (!supplier.getVendorCode().equals(request.getVendorCode())
                && supplierRepository.existsByVendorCode(request.getVendorCode())) {
            throw new DuplicateResourceException("Supplier with code " + request.getVendorCode() + " already exists!");
        }

        supplierMapper.updateEntity(supplier, request);
        return supplierRepository.save(supplier);
    }

    @Override
    public Supplier setActive(Long id, boolean active) {
        Supplier supplier = getSupplierById(id);
        supplier.setActive(active);
        return supplierRepository.save(supplier);
    }
}
