package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.CustomerReferenceDTO;
import com.aerospace.finance_service.entity.CustomerReference;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.CustomerReferenceRepository;
import com.aerospace.finance_service.service.CustomerReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerReferenceServiceImpl implements CustomerReferenceService {

    private final CustomerReferenceRepository customerReferenceRepository;

    // ACTIVE-only: ON_HOLD is deliberately excluded from the picker too, same as
    // INACTIVE — a credit-hold customer shouldn't be casually invoiced/journaled
    // against from a dropdown. getById() stays unfiltered.
    @Override
    public List<CustomerReferenceDTO> getAll() {
        return customerReferenceRepository.findByStatus("ACTIVE").stream().map(this::toDTO).toList();
    }

    @Override
    public CustomerReferenceDTO getById(Long id) {
        return customerReferenceRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private CustomerReferenceDTO toDTO(CustomerReference c) {
        return CustomerReferenceDTO.builder()
                .id(c.getId()).customerName(c.getCustomerName()).customerCode(c.getCustomerCode())
                .status(c.getStatus())
                .build();
    }
}
