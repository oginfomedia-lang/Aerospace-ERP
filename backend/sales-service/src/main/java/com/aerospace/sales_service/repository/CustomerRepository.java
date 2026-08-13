package com.aerospace.sales_service.repository;

import com.aerospace.sales_service.entity.Customer;
import com.aerospace.sales_service.entity.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByStatus(CustomerStatus status);
    boolean existsByNameIgnoreCase(String name);
}