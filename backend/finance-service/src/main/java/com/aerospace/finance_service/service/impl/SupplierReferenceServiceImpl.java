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

    // Active-only: this feeds a picker dropdown, and nothing today needs an
    // "all, including inactive" view. getById() stays unfiltered — an existing
    // payment/journal line referencing a now-inactive supplier must still resolve.
    @Override
    public List<SupplierReferenceDTO> getAll() {
        return supplierReferenceRepository.findByActiveTrue().stream().map(this::toDTO).toList();
    }

    @Override
    public SupplierReferenceDTO getById(Long id) {
        return supplierReferenceRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
    }

    private SupplierReferenceDTO toDTO(SupplierReference s) {
        return SupplierReferenceDTO.builder()
                .id(s.getId()).supplierName(s.getSupplierName()).vendorCode(s.getVendorCode())
                .active(s.getActive())
                .build();
    }
}
