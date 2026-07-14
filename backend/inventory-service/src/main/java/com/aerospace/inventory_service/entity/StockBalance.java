package com.aerospace.inventory_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stock_balance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long partId; // Links to Part Master
    
    @Column(nullable = false)
    private String batchNumber; // Critical for Aerospace traceability

    private Integer quantity; // The quantity for THIS specific batch/status combo

    @Enumerated(EnumType.STRING)
    private StockStatus status; // PENDING_QA (Blocked), APPROVED (Available), REJECTED

    private LocalDateTime expiryDate;//expiry tracking
    private String uom; // Unit of Measure, e.g., "EA", "KG", "LTR"

    private String location; // e.g., "WH1-ZONE-A-BIN12"

    // Soft reference to the procurement-service PO this batch was received against —
    // null for stock added outside a GRN (manual adjustments, initial stock entry).
    private String poNumber;

    private LocalDateTime lastUpdated;

    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }
}
