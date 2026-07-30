package com.aerospace.common.approval;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Immutable approval trail — one row per level per submission, for ANY module's document.
// documentId is a soft reference (same pattern used elsewhere in this codebase, e.g.
// Part.preferredSupplierId) — not a JPA foreign key, since common-lib has no single
// "documents" table to point to.
@Entity
@Table(name = "approval_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String moduleCode;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private int approvalLevel;

    @Column(nullable = false)
    private String approverRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalActionStatus status;

    private String approverUsername;
    private LocalDateTime approvedAt;

    @Column(length = 1000)
    private String remarks;

    private String digitalSignatureHash;

    @Column(nullable = false)
    private int documentVersion;

    //when this row become PENDING (either the first level on sub,=mit, or "woken up" from WAITING  when the prior level cleared)
    //the clock escalation measures against.
    private LocalDateTime pendingSince;

    @Builder.Default
    private boolean escalated = false;
    private LocalDateTime escalatedAt;

}