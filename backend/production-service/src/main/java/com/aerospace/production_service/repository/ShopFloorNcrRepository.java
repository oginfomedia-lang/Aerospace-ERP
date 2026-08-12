package com.aerospace.production_service.repository;

import com.aerospace.production_service.entity.NcrStatus;
import com.aerospace.production_service.entity.ShopFloorNcr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopFloorNcrRepository extends JpaRepository<ShopFloorNcr, Long> {
    List<ShopFloorNcr> findByProductionOrderId(Long productionOrderId);
    List<ShopFloorNcr> findByProductionMaterial_Id(Long productionMaterialId);
    List<ShopFloorNcr> findByProductionOrderIdAndStatus(Long productionOrderId, NcrStatus status);
    Optional<ShopFloorNcr> findByReworkStageLogId(Long reworkStageLogId);
}
