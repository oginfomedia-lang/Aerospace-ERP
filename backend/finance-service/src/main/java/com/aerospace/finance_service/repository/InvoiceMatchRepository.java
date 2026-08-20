package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.InvoiceMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceMatchRepository extends JpaRepository<InvoiceMatch, Long> {
    List<InvoiceMatch> findByTenantId(String tenantId);
    List<InvoiceMatch> findByInvoiceNumber(String invoiceNumber);
    List<InvoiceMatch> findByPoNumber(String poNumber);
}
