package com.aerospace.procurement_service.repository;

import com.aerospace.procurement_service.entity.VendorInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorInvoiceRepository extends JpaRepository<VendorInvoice, Long> {
}
