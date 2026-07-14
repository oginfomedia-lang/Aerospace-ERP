package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

// Approved Vendor List — which suppliers (procurement-service, soft-referenced by ID,
// same cross-service pattern as Part.preferredSupplierId) are approved to supply a
// given Part. At most one row per part has preferred=true, kept in sync with
// Part.preferredSupplierId by PartSupplierServiceImpl.
@Entity
@Table(name = "part_suppliers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartSupplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long partId;
    private Long supplierId;

    @Builder.Default
    private boolean preferred = false;
}
