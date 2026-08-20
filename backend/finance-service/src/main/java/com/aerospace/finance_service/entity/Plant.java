package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "plants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String plantCode;

    @Column(nullable = false, length = 150)
    private String plantName;

    // soft link, no cross-table FK
    private Long organizationId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activeStatus = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
