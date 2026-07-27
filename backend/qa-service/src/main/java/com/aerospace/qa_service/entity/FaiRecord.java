package com.aerospace.qa_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// AS9102-lite First Article Inspection record — auto-queued PENDING on a part's first
// GRN receipt, or opened manually ahead of production.
@Entity
@Table(name = "fai_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaiRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String faiNumber;

    @Column(nullable = false)
    private Long partId;

    private String partName;
    private String skuCode;

    // Freeform — Part has no revision field in this system, so this is descriptive
    // metadata typed by the inspector at perform-time, not a real dedupe key.
    private String partRevision;

    private String batchNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FaiStatus status = FaiStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private FaiResult overallResult; // null until COMPLETE

    private String materialCertRef;
    private String inspectorName;
    private String equipmentAssetTag;

    private LocalDateTime performedAt;

    @Column(length = 2000)
    private String remarks;

    private String signatureHash;

    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "faiRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FaiCharacteristic> characteristics = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
