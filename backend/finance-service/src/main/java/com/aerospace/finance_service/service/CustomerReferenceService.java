package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.CustomerReferenceDTO;

import java.util.List;

public interface CustomerReferenceService {
    List<CustomerReferenceDTO> getAll();
    CustomerReferenceDTO getById(Long id);
}
