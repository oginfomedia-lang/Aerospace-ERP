package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multi-tenant isolation: every record belongs to exactly one tenant.
    // Stamped by service layer from TenantContext on creation; never changes after that.
    @Column(length = 50)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 30)
    private String journalNumber; // DocumentSequenceService.generate("JE") -> "JE-2026-000001"

    @Column(nullable = false)
    private LocalDate postingDate;

    @Column(length = 500)
    private String narration;

    // loose/soft reference to whatever triggered this entry — NO cross-service FK,
    // same convention as every other cross-service reference in this repo
    @Column(length = 50)
    private String sourceDocType;   // e.g. "VENDOR_INVOICE", "SALES_DISPATCH", "MANUAL"
    @Column(length = 50)
    private String sourceDocRef;

    private Long costCenterId;
    private Long profitCenterId;
    private Long organizationId;
    private Long plantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JournalStatus status = JournalStatus.DRAFT;

    @Column(nullable = false, length = 100)
    private String createdBy;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime postedAt;

    // both directions tracked, for a full audit trail
    private Long reversalOfJournalId;
    private Long reversedByJournalId;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public enum JournalStatus {
        DRAFT,               // editable, being built
        PENDING_APPROVAL,    // submitted, awaiting ApprovalEngine sign-off
        REJECTED,            // sent back for correction
        POSTED                // approved AND committed — immutable from here (ADR-DDS-057)
    }
}
