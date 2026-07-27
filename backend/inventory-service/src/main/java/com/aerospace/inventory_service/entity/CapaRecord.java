package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "capa_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapaRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String capaNumber;

    // Real same-service FK — CAPA always belongs to exactly one NCR.
    @ManyToOne
    @JoinColumn(name = "ncr_id", nullable = false)
    private NonConformanceReport ncr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapaType capaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CapaStatus status = CapaStatus.OPEN;

    @Column(length = 2000)
    private String rootCause;
    @Column(length = 2000)
    private String correctiveAction;
    @Column(length = 2000)
    private String preventiveAction;

    private LocalDateTime targetCloseDate;
    private String ownerName;

    private String verificationRemarks;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime closedAt;

    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
