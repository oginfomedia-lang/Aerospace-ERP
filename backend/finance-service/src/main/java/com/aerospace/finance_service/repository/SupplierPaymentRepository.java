package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
    List<SupplierPayment> findByTenantId(String tenantId);
    List<SupplierPayment> findBySupplierRef(String supplierRef);
    List<SupplierPayment> findByInvoiceNumber(String invoiceNumber);
}
