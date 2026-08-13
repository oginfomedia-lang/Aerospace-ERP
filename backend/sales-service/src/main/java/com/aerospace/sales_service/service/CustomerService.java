package com.aerospace.sales_service.service;

import com.aerospace.sales_service.dto.CustomerRequestDTO;
import com.aerospace.sales_service.dto.CustomerResponseDTO;
import java.util.List;

public interface CustomerService {
    CustomerResponseDTO registerCustomer(CustomerRequestDTO dto);
    CustomerResponseDTO updateCustomer(Long id, CustomerRequestDTO dto);
    List<CustomerResponseDTO> listCustomers(String status);
    CustomerResponseDTO getCustomerById(Long id);
}