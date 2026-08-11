package com.aerospace.production_resource_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// An advance booking of a Machine for a future time window — distinct from
// Machine.occupancy (which only ever answers "is it busy RIGHT NOW"). A reservation
// existing does not flip occupancy; occupancy still only changes when
// MachineServiceImpl.occupyMachine/releaseMachine actually run at execution time.
@Entity
@Table(name = "machine_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    @ToString.Exclude
    private Machine machine;

    @Column(nullable = false)
    private LocalDateTime reservedFrom;

    @Column(nullable = false)
    private LocalDateTime reservedTo;

    // Loose cross-service reference into production-service — no FK, same pattern as
    // every other cross-service id in this app.
    private Long productionOrderId;

    @Column(nullable = false)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

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
