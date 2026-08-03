package com.aerospace.common.rules;

import jakarta.persistence.*;
import lombok.*;

// Admin-configurable "if field OP value then require role" check — chapter 7.8's
// example ("Quality-critical items require Quality approval") without hardcoding it.
@Entity
@Table(name = "business_rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String moduleCode;      // e.g. "QA_RELEASE"

    @Column(nullable = false)
    private String fieldName;       // e.g. "criticality" — matched against the caller-supplied facts map

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleOperator operator;

    @Column(nullable = false)
    private String value;           // e.g. "HIGH", or "HIGH,CRITICAL" for IN (comma-split)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleAction action;

    @Column(nullable = false)
    private String actionValue;     // the required role, e.g. "QA_INSPECTOR"

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
