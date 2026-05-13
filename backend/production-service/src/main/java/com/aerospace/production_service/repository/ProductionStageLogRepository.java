package com.aerospace.production_service.repository;

import com.aerospace.production_service.entity.ProductionStageLog;
import com.aerospace.production_service.entity.ProductionStageLog.StageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionStageLogRepository extends JpaRepository<ProductionStageLog, Long> {

    List<ProductionStageLog> findByProductionOrderIdOrderByLoggedAtAsc(Long productionOrderId);

    List<ProductionStageLog> findByProductionOrderIdAndStatus(Long productionOrderId, StageStatus status);

    List<ProductionStageLog> findByStageName(String stageName);
}
