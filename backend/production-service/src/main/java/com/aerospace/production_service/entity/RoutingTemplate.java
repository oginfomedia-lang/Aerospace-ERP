package com.aerospace.production_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Master data: the ordered sequence of manufacturing operations (Cutting, Drilling, ...)
// a part/product code must pass through. Matched against ProductionMaterial.partNumber
// when a ProductionOrder's routing is expanded (see ProductionOrderServiceImpl.startProduction).
@Entity
@Table(name = "routing_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String partNumber;

    @Column(nullable = false)
    private String name;

    private String description;

    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "routingTemplate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<RoutingOperation> operations = new ArrayList<>();

    // Optimistic locking — two engineers editing the same template at once now fail
    // loudly instead of one silently overwriting the other's changes.
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isActive == null) this.isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
