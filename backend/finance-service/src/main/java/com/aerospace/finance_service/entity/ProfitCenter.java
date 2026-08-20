package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "profit_centers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfitCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multi-tenant isolation: every record belongs to exactly one tenant.
    // Stamped by service layer from TenantContext on creation; never changes after that.
    @Column(length = 50)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 30)
    private String profitCenterCode;

    @Column(nullable = false, length = 150)
    private String profitCenterName;

    // soft link, no cross-table FK
    private Long plantId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activeStatus = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
