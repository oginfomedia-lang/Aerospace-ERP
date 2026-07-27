package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "capa_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapaHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "capa_record_id", nullable = false)
    private CapaRecord capaRecord;

    @Enumerated(EnumType.STRING)
    private CapaStatus fromStatus; // null on the very first (OPEN) row

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapaStatus toStatus;

    private String actor;

    @Column(length = 1000)
    private String remarks;

    private String signatureHash;

    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        this.occurredAt = LocalDateTime.now();
    }
}
