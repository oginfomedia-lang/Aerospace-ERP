package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_requistions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequisition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String prNumber;

    // Denormalized part identity — inventory-service's Part, not a JPA relation
    // (no cross-service FK anywhere in this repo).
    private Long partId;
    private String partName;
    private String skuCode;

    @Column(nullable = false)
    private Integer quantityNeeded;

    private LocalDateTime requiredByDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PRSourceType sourceType;

    private Long sourceWorkOrderId;
    private String sourceWoNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PRStatus status;

    private String remarks;
    private String createdBy;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}
