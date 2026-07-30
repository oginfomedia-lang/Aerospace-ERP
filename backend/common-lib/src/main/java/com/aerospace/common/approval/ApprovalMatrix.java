package com.aerospace.common.approval;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

// Amount-banded approval configuration, shared by every module (chapter 7.6/7.7).
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
    private String moduleCode; // e.g. "PURCHASE_ORDER", "VENDOR_INVOICE"

    @Column(nullable = false)
    private BigDecimal amountFrom;

    @Column(nullable = false)
    private BigDecimal amountTo;

    @Column(nullable = false)
    private String approverRole;

    @Column(nullable = false)
    private int levelSequence;

    @Builder.Default
    @ColumnDefault("true")
    @Column(nullable = false)
    @JsonProperty("isActive")
    private boolean isActive = true;

    // If set, a PENDING row at this level auto-escalates after this many hours.
    // Null = no escalation for this band (opt-in, per level).
    private Integer escalationHours;

    // If set, escalating reassigns the pending row to this role instead of just
    // flagging it. Null = flag only (a "reminder"), approver stays the same.
    private String escalationToRole;

    private String approverUsername;

    private String parallelGroup;
}