package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.CollectionActivityCreateDTO;
import com.aerospace.finance_service.dto.CollectionActivityDTO;

import java.util.List;

public interface CollectionService {
    CollectionActivityDTO logActivity(CollectionActivityCreateDTO dto, String currentUser);
    List<CollectionActivityDTO> getByCustomer(String customerRef);
    List<CollectionActivityDTO> getAll();
    CollectionActivityDTO resolve(Long id);
    CollectionActivityDTO escalate(Long id);
}
