package com.aerospace.procurement_service.repository;

import com.aerospace.procurement_service.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByVendorCode(String vendorCode);
    boolean existsByVendorCode(String vendorCode);
}
