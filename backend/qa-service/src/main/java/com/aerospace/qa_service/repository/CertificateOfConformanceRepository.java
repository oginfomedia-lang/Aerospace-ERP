package com.aerospace.qa_service.repository;

import com.aerospace.qa_service.entity.CertificateOfConformance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateOfConformanceRepository extends JpaRepository<CertificateOfConformance, Long> {
    List<CertificateOfConformance> findByBatchNumberOrderByIssuedAtDesc(String batchNumber);
    List<CertificateOfConformance> findByPartIdOrderByIssuedAtDesc(Long partId);
    Optional<CertificateOfConformance> findByInspectionReportId(Long inspectionReportId);
    boolean existsByInspectionReportId(Long inspectionReportId);
}
