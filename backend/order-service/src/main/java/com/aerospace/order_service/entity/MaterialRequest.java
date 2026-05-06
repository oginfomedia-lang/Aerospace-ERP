package com.aerospace.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    // References inventory-service part ID
    private Long partId;
    private String partName;
    private String batchNumber;

    private int quantityRequested;
    private int quantityIssued;    // updated when inventory confirms issue

    private String location;       // bin location to pull from

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialRequestStatus status;

    private String remarks;
    private LocalDateTime requestedAt;
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
        if (this.status == null) this.status = MaterialRequestStatus.PENDING;
    }
}