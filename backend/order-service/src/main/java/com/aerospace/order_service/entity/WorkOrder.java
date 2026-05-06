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

    // e.g. WO-2026-001 — generated on creation
    @Column(nullable = false, unique = true)
    private String workOrderNumber;

    @Column(nullable = false)
    private String title;

    private String description;

    // Aircraft registration or asset tag this WO is against
    private String aircraftId;

    // Task reference — e.g. AMM task number in real aerospace
    private String taskReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderType type;

    private String assignedTo;       // technician name or team
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime scheduledDate;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime dueDate;

    private String completionNotes;
    private String rejectionReason;

    // One WO can request multiple material lines
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MaterialRequest> materialRequests = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = WorkOrderStatus.DRAFT;
        if (this.priority == null) this.priority = WorkOrderPriority.MEDIUM;
    }
}