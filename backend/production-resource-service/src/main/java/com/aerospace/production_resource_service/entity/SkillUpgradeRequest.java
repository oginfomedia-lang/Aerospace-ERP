package com.aerospace.production_resource_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// One row per upgrade request. Starts PENDING and is later moved to APPROVED/REJECTED
// in place — same single-lifecycle shape as EmployeeLeave, not the append-only
// EmployeeSkillCertification history pattern. Approval writes an actual
// EmployeeSkillCertification row (see EmployeeServiceImpl.recordSkillCertificationInternal);
// this table only tracks the request/decision, never the certification data itself.
@Entity
@Table(name = "skill_upgrade_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillUpgradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @ToString.Exclude
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    @ToString.Exclude
    private Skill skill;

    @Column(nullable = false)
    private Integer fromLevel;

    @Column(nullable = false)
    private Integer toLevel;

    private String reason;
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SkillUpgradeStatus status = SkillUpgradeStatus.PENDING;

    private String decidedBy;
    private LocalDateTime decidedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
