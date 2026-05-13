package com.aerospace.production_service.repository;

import com.aerospace.production_service.entity.ProductionMaterial;
import com.aerospace.production_service.entity.ProductionMaterial.MaterialStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionMaterialRepository extends JpaRepository<ProductionMaterial, Long> {

    List<ProductionMaterial> findByProductionOrderId(Long productionOrderId);

    List<ProductionMaterial> findByPartId(Long partId);

    List<ProductionMaterial> findByBatchNumber(String batchNumber);

    List<ProductionMaterial> findByStatus(MaterialStatus status);

    List<ProductionMaterial> findByProductionOrderIdAndStatus(
            Long productionOrderId, MaterialStatus status);
}
