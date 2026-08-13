package com.aerospace.sales_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dispatches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String dispatchNumber; // e.g. DISP-2026-000001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    @ToString.Exclude
    private SalesOrder salesOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DispatchStatus status = DispatchStatus.PENDING;

    private String carrierName;
    private String trackingNumber;
    private String packedBy;
    private String dispatchedBy;

    private LocalDateTime packedAt;
    private LocalDateTime dispatchedAt;

    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<DispatchLine> lines = new ArrayList<>();

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}
