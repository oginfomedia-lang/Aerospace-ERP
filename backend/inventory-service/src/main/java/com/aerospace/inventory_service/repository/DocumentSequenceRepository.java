package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.DocumentSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    // Row-level lock prevents two concurrent requests from generating the same number (ARC-BR-006)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentSequence d where d.docType = :docType and d.yearKey = :yearKey")
    Optional<DocumentSequence> lockByDocTypeAndYearKey(String docType, String yearKey);
}
