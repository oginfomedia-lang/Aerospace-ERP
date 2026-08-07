package com.aerospace.production_resource_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// One row per production machine. lastMaintenanceAt/nextMaintenanceDueAt are
// denormalized from the latest MachineMaintenanceRecord so due/overdue lists and
// badges don't need a join+aggregate on every read — same reasoning as Equipment.
@Entity
@Table(name = "machines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String machineCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MachineType machineType;

    private String manufacturer;
    private String modelNumber;
    private String location;
    private String capacity; // freeform, e.g. "500x300x400mm" or "5-axis, 12000 RPM"

    @Column(nullable = false)
    private Integer maintenanceFrequencyDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MachineStatus status = MachineStatus.ACTIVE;

    // Optional loose reference to a qa-service Equipment record, for machines with a
    // calibratable sub-component (e.g. a CMM probe on a mill). No FK — cross-service.
    private Long linkedEquipmentId;

    private LocalDateTime lastMaintenanceAt;
    private LocalDateTime nextMaintenanceDueAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
