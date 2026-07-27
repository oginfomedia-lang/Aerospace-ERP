package com.aerospace.qa_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// One row per calibrated gauge/tool/instrument the shop tracks.
// lastCalibratedAt/nextCalibrationDueAt are denormalized from the latest CalibrationRecord
// so the due/overdue list and badges don't need a join+aggregate on every read.
@Entity
@Table(name = "qa_equipment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Physical tag stuck on the gauge — user-entered, not system-generated (unlike
    // FAI/CoC document numbers), since it identifies a physical asset, not a document.
    @Column(nullable = false, unique = true)
    private String assetTag;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentType equipmentType;

    private String manufacturer;
    private String modelNumber;
    private String location;

    @Column(nullable = false)
    private Integer calibrationFrequencyDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EquipmentStatus status = EquipmentStatus.ACTIVE;

    // Denormalized from the latest CalibrationRecord — null until first calibrated.
    private LocalDateTime lastCalibratedAt;
    private LocalDateTime nextCalibrationDueAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
