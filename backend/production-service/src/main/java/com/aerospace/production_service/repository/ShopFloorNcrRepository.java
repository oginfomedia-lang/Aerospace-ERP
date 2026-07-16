package com.aerospace.production_service.repository;

import com.aerospace.production_service.entity.ShopFloorNcr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopFloorNcrRepository extends JpaRepository<ShopFloorNcr, Long> {
    List<ShopFloorNcr> findByProductionOrderId(Long productionOrderId);
    List<ShopFloorNcr> findByProductionMaterial_Id(Long productionMaterialId);
}
