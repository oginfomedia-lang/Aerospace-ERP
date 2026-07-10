package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

// Amount-banded approval configuration (PUR-BR-001 / sec_approval_matrix).
@Entity
@Table(name = "approval_matrix")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalMatrix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String moduleCode; // e.g. "PURCHASE_ORDER"

    @Column(nullable = false)
    private BigDecimal amountFrom;

    @Column(nullable = false)
    private BigDecimal amountTo;

    @Column(nullable = false)
    private String approverRole; // matches a RoleName from auth-service, e.g. "MANAGER"

    @Column(nullable = false)
    private int levelSequence;

    @Builder.Default
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean isActive = true;
}
