package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.Plant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant, Long> {
    Optional<Plant> findByPlantCode(String plantCode);
    List<Plant> findByActiveStatusTrue();
    boolean existsByPlantCode(String plantCode);
}
