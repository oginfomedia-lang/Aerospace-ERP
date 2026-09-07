package com.aerospace.procurement_service.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aerospace.procurement_service.entity.VendorInvoice;

@Repository
public interface VendorInvoiceRepository extends JpaRepository<VendorInvoice, Long> {

   // Row-locked read for approve/reject/markPaid — same pattern as
   // PurchaseOrderRepository.lockById, prevents two concurrent actions (double-click, or
   // a retry) from both reading the same pre-transition status and double-firing AP posting.
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select v from VendorInvoice v where v.id = :id")
   Optional<VendorInvoice> lockById(@Param("id") Long id);
}
