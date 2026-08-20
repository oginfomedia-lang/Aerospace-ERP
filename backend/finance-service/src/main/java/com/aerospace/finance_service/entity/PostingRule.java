package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "posting_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multi-tenant isolation: every record belongs to exactly one tenant.
    // Stamped by service layer from TenantContext on creation; never changes after that.
    @Column(length = 50)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PostingTriggerType triggerEvent;

    @Column(nullable = false, length = 30)
    private String debitAccountCode;
    @Column(nullable = false, length = 30)
    private String creditAccountCode;
    @Column(length = 30)
    private String taxRuleCode; // nullable — not every event carries tax
    private Long costCenterId;   // nullable — default cost center to tag postings with
    private Long profitCenterId; // nullable

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // exactly one active rule per triggerEvent expected

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
