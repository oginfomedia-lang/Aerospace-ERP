package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Value stored on StockBalance.location / FormGrnRequest.location — the
    // stable identifier other tables reference (loose reference, not a DB FK,
    // same convention already used for Part.preferredSupplierId).
    @Column(unique = true, nullable = false)
    private String code; // e.g. "WH1-RACK-2"

    @Column(nullable = false)
    private String name; // e.g. "Warehouse 1 - Rack 2"

    private String zone; // optional grouping, e.g. "Warehouse 1"

    // Soft delete (ARC-BR-003 pattern, same as Supplier) — deactivated locations
    // stay for history on existing stock/GRN records but drop out of the dropdown.
    @Builder.Default
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;
}
