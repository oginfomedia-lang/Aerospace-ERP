package com.aerospace.sales_service.service.impl;

import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.sales_service.dto.CustomerRequestDTO;
import com.aerospace.sales_service.dto.CustomerResponseDTO;
import com.aerospace.sales_service.entity.Customer;
import com.aerospace.sales_service.entity.CustomerStatus;
import com.aerospace.sales_service.event.producer.SalesProducer;
import com.aerospace.sales_service.exception.ResourceNotFoundException;
import com.aerospace.sales_service.repository.CustomerRepository;
import com.aerospace.sales_service.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final DocumentSequenceService documentSequenceService;
    private final SalesProducer salesProducer;

    @Override
    @Transactional
    public CustomerResponseDTO registerCustomer(CustomerRequestDTO dto) {
        Customer customer = Customer.builder()
                .customerCode(documentSequenceService.generate("CUST"))
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .billingAddress(dto.getBillingAddress())
                .shippingAddress(dto.getShippingAddress())
                .currency(dto.getCurrency())
                .paymentTerms(dto.getPaymentTerms())
                .status(CustomerStatus.ACTIVE)
                .build();
        Customer saved = customerRepository.save(customer);
        log.info("Customer registered — customerCode: {}, name: {}", saved.getCustomerCode(), saved.getName());
        salesProducer.sendCustomerRegistered(saved.getId(), saved.getCustomerCode(), saved.getName());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public CustomerResponseDTO updateCustomer(Long id, CustomerRequestDTO dto) {
        Customer customer = findOrThrow(id);
        customer.setName(dto.getName());
        customer.setContactPerson(dto.getContactPerson());
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setBillingAddress(dto.getBillingAddress());
        customer.setShippingAddress(dto.getShippingAddress());
        customer.setCurrency(dto.getCurrency());
        customer.setPaymentTerms(dto.getPaymentTerms());
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            customer.setStatus(CustomerStatus.valueOf(dto.getStatus()));
        }
        Customer saved = customerRepository.save(customer);
        salesProducer.sendCustomerUpdated(saved.getId(), saved.getCustomerCode(), saved.getName(), saved.getStatus().name());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> listCustomers(String status) {
        List<Customer> customers = status == null
                ? customerRepository.findAll()
                : customerRepository.findByStatus(CustomerStatus.valueOf(status));
        return customers.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerById(Long id) {
        return toDTO(findOrThrow(id));
    }

    private Customer findOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
    }

    private CustomerResponseDTO toDTO(Customer c) {
        return CustomerResponseDTO.builder()
                .id(c.getId())
                .customerCode(c.getCustomerCode())
                .name(c.getName())
                .contactPerson(c.getContactPerson())
                .email(c.getEmail())
                .phone(c.getPhone())
                .billingAddress(c.getBillingAddress())
                .shippingAddress(c.getShippingAddress())
                .currency(c.getCurrency())
                .paymentTerms(c.getPaymentTerms())
                .status(c.getStatus().name())
                .version(c.getVersion())
                .createdAt(c.getCreatedAt())
                .build();
    }
}