package com.aerospace.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. WO-2024-00142 — auto-generated on create
    @Column(nullable = false, unique = true)
    private String woNumber;

    // Aircraft or system this WO is for
    @Column(nullable = false)
    private String aircraftId;

    private String aircraftType;   // e.g. Boeing 737, Airbus A320

    @Column(nullable = false)
    private String taskDescription;

    private String taskCode;       // ATA chapter code e.g. ATA-32-00

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    private String assignedTo;     // technician name / team
    private String createdBy;
    private String location;       // hangar / bay

    private LocalDateTime dueDate;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String remarks;
    private String closureNotes;

    // One WO has many material line items
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<MaterialLineItem> materialLineItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = WorkOrderStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}