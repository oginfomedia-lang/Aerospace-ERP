package com.aerospace.common.numbering;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentSequence d where d.docType = :docType and d.yearKey = :yearKey " +
           "and d.companyCode = :companyCode and d.plantCode = :plantCode")
    Optional<DocumentSequence> lockRow(@Param("docType") String docType, @Param("yearKey") String yearKey,
            @Param("companyCode") String companyCode, @Param("plantCode") String plantCode);
}