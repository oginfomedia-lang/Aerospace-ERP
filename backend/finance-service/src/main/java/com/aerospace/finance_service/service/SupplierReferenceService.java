package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.SupplierReferenceDTO;

import java.util.List;

public interface SupplierReferenceService {
    List<SupplierReferenceDTO> getAll();
    SupplierReferenceDTO getById(Long id);
}
