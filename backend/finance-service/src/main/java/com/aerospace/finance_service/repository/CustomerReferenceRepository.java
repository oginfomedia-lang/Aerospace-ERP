package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.CustomerReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerReferenceRepository extends JpaRepository<CustomerReference, Long> {
    Optional<CustomerReference> findByCustomerCode(String customerCode);
    List<CustomerReference> findByStatus(String status);
}
