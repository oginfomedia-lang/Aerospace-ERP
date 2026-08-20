package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.InvoiceMatchDTO;
import com.aerospace.finance_service.dto.InvoiceMatchRequestDTO;

import java.util.List;

public interface InvoiceMatchService {
    InvoiceMatchDTO match(InvoiceMatchRequestDTO request, String currentUser);
    List<InvoiceMatchDTO> getAll();
    InvoiceMatchDTO getById(Long id);
}
