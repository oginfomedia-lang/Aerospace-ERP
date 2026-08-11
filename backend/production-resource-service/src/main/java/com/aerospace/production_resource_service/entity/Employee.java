package com.aerospace.production_resource_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String name;

    private String designation;
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    // Skills are no longer a flat set here — see EmployeeSkillCertification (queried
    // via its own repository, same pattern Machine uses for MachineMaintenanceRecord,
    // not a collection field on this entity) for level/certification/expiry/history.

    // Which Shift this employee normally works, if any — a real relationship (unlike
    // Machine.linkedEquipmentId, which is cross-SERVICE and can't be a JPA relation).
    // Shift lives in this same service/database, so a proper @ManyToOne is the correct
    // choice here — it lets occupyEmployee read employee.getShift() directly without a
    // separate repository lookup. Null means "no shift assigned yet", which
    // occupyEmployee treats as "no shift-window restriction," not a block.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    @ToString.Exclude
    private Shift shift;

    // Busy-right-now tracking — separate from `status` above on purpose, same
    // reasoning as Machine.occupancy. Set/cleared automatically by production-service
    // when a stage using this employee starts/ends.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmployeeOccupancy occupancy = EmployeeOccupancy.FREE;

    // Which ProductionStageLog currently holds this employee, if any.
    private Long currentStageLogId;

    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
