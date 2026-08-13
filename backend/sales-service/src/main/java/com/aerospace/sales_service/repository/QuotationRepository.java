package com.aerospace.sales_service.repository;

import com.aerospace.sales_service.entity.Quotation;
import com.aerospace.sales_service.entity.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByCustomerId(Long customerId);
    List<Quotation> findByStatus(QuotationStatus status);
    Optional<Quotation> findByQuotationNumber(String quotationNumber);
}
