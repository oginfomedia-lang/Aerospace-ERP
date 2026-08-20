package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.SupplierPaymentCreateDTO;
import com.aerospace.finance_service.dto.SupplierPaymentDTO;

import java.util.List;

public interface SupplierPaymentService {
    SupplierPaymentDTO create(SupplierPaymentCreateDTO dto, String currentUser);
    SupplierPaymentDTO cancel(Long id, String currentUser);
    List<SupplierPaymentDTO> getAll();
    SupplierPaymentDTO getById(Long id);
}
