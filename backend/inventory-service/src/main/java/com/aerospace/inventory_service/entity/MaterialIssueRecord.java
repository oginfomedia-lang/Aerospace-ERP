package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

// The outbound counterpart to MaterialReceipt — a durable, printable record of exactly
// which batches (and how much from each) fed a single issue-to-production call, since
// PartServiceImpl.issueToProduction's FIFO split would otherwise only live as a
// flattened "B001:20, B002:30" string on the MaterialIssuedEvent.
@Entity
@Table(name = "material_issue_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialIssueRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String issueNumber; // system-generated, e.g. ISS/2026/00001

    private Long partId;
    private String partName;
    private Integer totalQuantityIssued;

    private Long workOrderId;
    private String woNumber;
    private Long lineItemId;

    private String issuedTo;
    private String issuedBy;
    private LocalDateTime issuedAt;

    @OneToMany(mappedBy = "materialIssueRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MaterialIssueRecordLine> lines = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.issuedAt == null) this.issuedAt = LocalDateTime.now();
    }
}
