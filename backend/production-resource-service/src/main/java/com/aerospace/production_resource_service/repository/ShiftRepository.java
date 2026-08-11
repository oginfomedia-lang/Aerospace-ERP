package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    boolean existsByName(String name);
}
