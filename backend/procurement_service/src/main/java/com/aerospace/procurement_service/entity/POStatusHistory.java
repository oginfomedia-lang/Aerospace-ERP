package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "po_status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long purchaseOrderId;

    private String poNumber;

    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus toStatus;

    private String changedBy;
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}