package com.aerospace.qa_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// Append-only — one row per calibration event, never edited after the fact
// (same "immutable history row" idea as procurement-service's POApprovalHistory).
@Entity
@Table(name = "qa_calibration_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false)
    private LocalDateTime calibratedAt;

    private String performedBy;

    // External cal-lab certificate reference — freeform, same convention as
    // InspectionReport.certificateNumber.
    private String certificateNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalibrationResult result;

    @Column(length = 2000)
    private String remarks;

    // Server-computed = calibratedAt + equipment.calibrationFrequencyDays at record time —
    // never trust a client-sent due date.
    @Column(nullable = false)
    private LocalDateTime nextDueAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
