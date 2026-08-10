package com.aerospace.production_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "routing_operations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_template_id", nullable = false)
    @ToString.Exclude
    private RoutingTemplate routingTemplate;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false)
    private String operationName; // e.g. "Cutting", "Drilling"

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OperationType operationType = OperationType.PRODUCTION;

    private String operationCode;
    private Integer expectedDurationMinutes;
    private String workstation;
    private String instructions;

    // Loose cross-service references into production-resource-service — no FK, no
    // validation call. The denormalized code/name is populated by the frontend
    // alongside the id whenever the user picks from a dropdown, same as how
    // partNumber selection already auto-fills Name/Description elsewhere.
    private Long preferredMachineId;
    private String preferredMachineCode;
    private Long preferredEmployeeId;
    private String preferredEmployeeName;
}
