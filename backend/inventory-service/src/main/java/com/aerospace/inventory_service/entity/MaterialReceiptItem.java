package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_receipt_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialReceiptItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "material_receipt_id")
    private MaterialReceipt materialReceipt;

    private String skuCode;
    private Integer quantity;
    private String uom;

    // Populated once form-grn actually posts this item's stock (GRN-FR-001: system-generated)
    private String grnNumber;

    // Snapshotted from FormGrnRequest at GRN-formation time (see formGrn()) — previously
    // only used transiently to post StockBalance, never persisted for later redisplay
    // on the printed GRN document.
    private String batchNumber;
    private String location;
    private LocalDateTime expiryDate;
    private Integer inspectionQty;
}
