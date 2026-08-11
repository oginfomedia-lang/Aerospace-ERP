package com.aerospace.production_resource_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Append-only — one row per certification/evaluation EVENT for an employee+skill,
// never edited after the fact. Mirrors MachineMaintenanceRecord's pattern exactly:
// "current" state (level, whether still certified) is the latest row per
// (employee, skill), not a field that gets overwritten in place — so re-evaluating
// someone never destroys the record of what they held before.
//
// `level` is a plain 1-5 integer, not a configurable named lookup table (that's a
// natural follow-up if named levels ever need to be admin-editable) — by convention:
// 1=Beginner, 2=Basic, 3=Intermediate, 4=Advanced, 5=Expert.
@Entity
@Table(name = "employee_skill_certifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSkillCertification {

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
    private Integer level;

    private Integer experienceHours;

    private String certificateNumber;
    private LocalDate issuedDate;
    private LocalDate expiryDate;

    private String evaluatedBy;
    private LocalDate evaluationDate;
    private LocalDate nextEvaluationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CertificationStatus status = CertificationStatus.ACTIVE;

    @Column(length = 2000)
    private String remarks;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
