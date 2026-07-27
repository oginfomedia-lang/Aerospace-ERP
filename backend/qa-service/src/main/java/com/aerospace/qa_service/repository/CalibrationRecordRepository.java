package com.aerospace.qa_service.repository;

import com.aerospace.qa_service.entity.CalibrationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalibrationRecordRepository extends JpaRepository<CalibrationRecord, Long> {

    List<CalibrationRecord> findByEquipmentIdOrderByCalibratedAtDesc(Long equipmentId);
}
