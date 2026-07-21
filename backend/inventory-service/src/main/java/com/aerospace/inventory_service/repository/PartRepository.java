package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findBySkuCode(String skuCode);
    List<Part> findByNameContainingIgnoreCase(String name);
    List<Part> findByPartType(String type);
    Page<Part> findAll(Pageable pageable);

    @Query("SELECT p.partType, COUNT(p) FROM Part p GROUP BY p.partType")
    List<Object[]> countPartsByType();

    // Deactivated parts are never candidates for reorder — excluding them stops a
    // fully-rejected/deactivated item with 0 approved stock from double-counting as
    // "Low Stock" too (that's a QA/lifecycle issue, not a supply shortage).
    @Query("SELECT p.id, p.reorderLevel FROM Part p WHERE p.reorderLevel IS NOT NULL AND p.status <> 'INACTIVE'")
    List<Object[]> findIdAndReorderLevelWhereReorderLevelSet();

}
