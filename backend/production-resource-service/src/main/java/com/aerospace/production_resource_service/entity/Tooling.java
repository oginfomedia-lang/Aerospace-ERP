package com.aerospace.production_resource_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// A cutting tool, fixture, jig, or measuring gauge — mirrors Machine's shape
// (register/occupy/release, optimistic locking) but has no maintenance history of
// its own in this pass; that's a natural follow-up if tool wear-tracking is needed.
@Entity
@Table(name = "tooling")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tooling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String toolCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToolingType toolingType;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ToolingStatus status = ToolingStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ToolingOccupancy occupancy = ToolingOccupancy.FREE;

    // Which ProductionStageLog currently holds this tool, if any — same purpose as
    // Machine.currentStageLogId.
    private Long currentStageLogId;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
