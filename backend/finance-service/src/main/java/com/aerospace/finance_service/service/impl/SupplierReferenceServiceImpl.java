package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.SupplierReferenceDTO;
import com.aerospace.finance_service.entity.SupplierReference;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.SupplierReferenceRepository;
import com.aerospace.finance_service.service.SupplierReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierReferenceServiceImpl implements SupplierReferenceService {

    private final SupplierReferenceRepository supplierReferenceRepository;

    @Override
    public List<SupplierReferenceDTO> getAll() {
        return supplierReferenceRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public SupplierReferenceDTO getById(Long id) {
        return supplierReferenceRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
    }

    private SupplierReferenceDTO toDTO(SupplierReference s) {
        return SupplierReferenceDTO.builder()
                .id(s.getId()).supplierName(s.getSupplierName()).vendorCode(s.getVendorCode())
                .build();
    }
}
