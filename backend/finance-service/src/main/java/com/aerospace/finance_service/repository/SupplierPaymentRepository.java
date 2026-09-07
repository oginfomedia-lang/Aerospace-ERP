package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.SupplierPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
    List<SupplierPayment> findByTenantId(String tenantId);
    List<SupplierPayment> findBySupplierRef(String supplierRef);
    List<SupplierPayment> findByInvoiceNumber(String invoiceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SupplierPayment p where p.id = :id")
    Optional<SupplierPayment> lockById(@Param("id") Long id);
}
