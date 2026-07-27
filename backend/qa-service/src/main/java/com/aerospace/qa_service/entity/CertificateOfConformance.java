package com.aerospace.qa_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_of_conformance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateOfConformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String cocNumber;

    @Column(nullable = false)
    private Long partId;

    private String partName;
    private String skuCode;

    @Column(nullable = false)
    private String batchNumber;

    // Soft reference, same convention as inventory-service's NCR.poNumber
    private String poNumber;

    @Column(nullable = false)
    private Integer quantityCertified;

    // Real same-service FK — the inspection this certificate is derived from
    @ManyToOne
    @JoinColumn(name = "inspection_report_id", nullable = false)
    private InspectionReport inspectionReport;

    private String certifyingStandard;
    private String issuedBy;
    private LocalDateTime issuedAt;
    private String signatureHash;

    @PrePersist
    protected void onCreate() {
        this.issuedAt = LocalDateTime.now();
    }
}
