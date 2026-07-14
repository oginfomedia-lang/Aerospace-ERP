package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCode(String code);
    boolean existsByCode(String code);
    List<Location> findByActiveTrue();
}
