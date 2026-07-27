package com.aerospace.qa_service.repository;

import com.aerospace.qa_service.entity.Equipment;
import com.aerospace.qa_service.entity.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByStatus(EquipmentStatus status);

    // Due/overdue list — used by the UI banner and the daily scheduler alike
    List<Equipment> findByNextCalibrationDueAtBefore(LocalDateTime cutoff);

    Optional<Equipment> findByAssetTag(String assetTag);

    boolean existsByAssetTag(String assetTag);
}
