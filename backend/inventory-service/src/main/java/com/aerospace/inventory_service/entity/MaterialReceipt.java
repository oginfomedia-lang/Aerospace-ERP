package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

// The physical gate/dock receipt of goods against a PO — recorded BEFORE any GRN or
// QC bucket exists. GRN is only ever formed FROM an existing MaterialReceipt
// (MaterialReceiptService.formGrn), never directly from a PO.
@Entity
@Table(name = "material_receipts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String mrNumber; // system-generated, e.g. MR/2026/00001

    private String poNumber; // soft reference — procurement-service owns the actual PO
    private String supplierName;
    private String challanNumber;
    private String transportName;
    private String receivedBy;
    private LocalDateTime receivedAt;

    // Set once, when formGrn() flips status to GRN_FORMED — distinct from receivedAt
    // (gate receipt time), since a GRN can be formed well after the goods arrived.
    private LocalDateTime grnFormedAt;

    @Enumerated(EnumType.STRING)
    private MaterialReceiptStatus status;

    @OneToMany(mappedBy = "materialReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MaterialReceiptItem> items = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.receivedAt == null) this.receivedAt = LocalDateTime.now();
        if (this.status == null) this.status = MaterialReceiptStatus.RECEIVED_AT_GATE;
    }
}
