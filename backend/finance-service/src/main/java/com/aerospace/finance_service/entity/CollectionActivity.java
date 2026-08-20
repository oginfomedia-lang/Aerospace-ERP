package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks each collection touchpoint for outstanding AR balances:
 * calls, emails, legal notices, and promises to pay. Provides an audit
 * trail for the collections team and management escalation reporting.
 */
@Entity
@Table(name = "collection_activities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollectionActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 30)
    private String activityNumber;  // DocumentSequenceService.generate("COL")

    @Column(nullable = false, length = 60)
    private String customerRef;     // e.g. "CUSTOMER:207"

    @Column(length = 50)
    private String invoiceRef;      // specific invoice being chased (optional)

    @Column(nullable = false)
    private LocalDate activityDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityType activityType;

    @Column(length = 1000)
    private String notes;

    private LocalDate promisedPaymentDate;  // date customer promised to pay

    @Column(precision = 18, scale = 2)
    private BigDecimal promisedAmount;

    private LocalDate followUpDate;  // when to follow up next

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ActivityStatus status = ActivityStatus.OPEN;

    @Column(nullable = false, length = 100)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ActivityType  { CALL, EMAIL, VISIT, LETTER, LEGAL_NOTICE, OTHER }
    public enum ActivityStatus { OPEN, RESOLVED, ESCALATED }
}
