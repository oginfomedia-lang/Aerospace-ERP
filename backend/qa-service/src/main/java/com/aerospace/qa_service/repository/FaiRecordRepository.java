package com.aerospace.qa_service.repository;

import com.aerospace.qa_service.entity.FaiRecord;
import com.aerospace.qa_service.entity.FaiStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FaiRecordRepository extends JpaRepository<FaiRecord, Long> {
    boolean existsByPartId(Long partId);
    List<FaiRecord> findByPartIdOrderByCreatedAtDesc(Long partId);
    List<FaiRecord> findByStatusOrderByCreatedAtDesc(FaiStatus status);
    Optional<FaiRecord> findByFaiNumber(String faiNumber);
}
