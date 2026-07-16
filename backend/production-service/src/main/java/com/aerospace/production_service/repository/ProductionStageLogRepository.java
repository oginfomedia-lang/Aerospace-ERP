package com.aerospace.production_service.repository;

import com.aerospace.production_service.entity.ProductionStageLog;
import com.aerospace.production_service.entity.ProductionStageLog.StageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionStageLogRepository extends JpaRepository<ProductionStageLog, Long> {

    List<ProductionStageLog> findByProductionOrderIdOrderByLoggedAtAsc(Long productionOrderId);

    List<ProductionStageLog> findByProductionOrderIdAndStatus(Long productionOrderId, StageStatus status);

    List<ProductionStageLog> findByStageName(String stageName);

    // ── Routing-driven, per-BOM-line stages ───────────────────────────────────
    List<ProductionStageLog> findByProductionMaterial_IdOrderBySequenceNumberAscAttemptNumberAsc(
            Long productionMaterialId);

    // Latest attempt for a given sequence step — gating always reads this, never an
    // earlier (superseded-by-REWORK) attempt.
    java.util.Optional<ProductionStageLog> findFirstByProductionMaterial_IdAndSequenceNumberOrderByAttemptNumberDesc(
            Long productionMaterialId, Integer sequenceNumber);

    // The single order-level stage (productionMaterial IS NULL) — Final Assembly.
    List<ProductionStageLog> findByProductionOrderIdAndProductionMaterialIsNull(Long productionOrderId);
}
