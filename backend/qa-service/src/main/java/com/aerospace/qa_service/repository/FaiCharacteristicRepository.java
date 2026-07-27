package com.aerospace.qa_service.repository;

import com.aerospace.qa_service.entity.FaiCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaiCharacteristicRepository extends JpaRepository<FaiCharacteristic, Long> {
    List<FaiCharacteristic> findByFaiRecordId(Long faiRecordId);
}
