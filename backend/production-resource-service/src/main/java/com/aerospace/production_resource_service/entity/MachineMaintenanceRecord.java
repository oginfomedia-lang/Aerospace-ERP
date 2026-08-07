package com.aerospace.production_resource_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Append-only — one row per maintenance event, never edited after the fact.
@Entity
@Table(name = "machine_maintenance_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineMaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceType maintenanceType;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    private String performedBy;
    private BigDecimal cost;
    private Integer downtimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceResult result;

    @Column(length = 2000)
    private String remarks;

    // Server-computed = performedAt + machine.maintenanceFrequencyDays — never trust
    // a client-sent due date, same rule CalibrationRecord.nextDueAt already follows.
    @Column(nullable = false)
    private LocalDateTime nextDueAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
