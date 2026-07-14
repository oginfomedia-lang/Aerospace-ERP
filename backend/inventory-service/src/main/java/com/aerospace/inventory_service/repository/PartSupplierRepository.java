package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.PartSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartSupplierRepository extends JpaRepository<PartSupplier, Long> {
    List<PartSupplier> findByPartId(Long partId);
    List<PartSupplier> findBySupplierId(Long supplierId);
    Optional<PartSupplier> findByPartIdAndSupplierId(Long partId, Long supplierId);
    void deleteByPartIdAndSupplierId(Long partId, Long supplierId);
}
