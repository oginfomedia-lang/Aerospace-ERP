package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.SupplierReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierReferenceRepository extends JpaRepository<SupplierReference, Long> {
    Optional<SupplierReference> findByVendorCode(String vendorCode);
}
