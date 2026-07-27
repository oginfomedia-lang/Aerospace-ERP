package com.aerospace.qa_service.repository;

import com.aerospace.qa_service.entity.DocumentSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentSequence d where d.docType = :docType and d.yearKey = :yearKey")
    Optional<DocumentSequence> lockByDocTypeAndYearKey(String docType, String yearKey);
}
