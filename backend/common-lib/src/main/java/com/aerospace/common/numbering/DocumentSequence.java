package com.aerospace.common.numbering;

import jakarta.persistence.*;
import lombok.*;

// One row per (docType, year, company, plant). Central numbering per chapter 7.9.
@Entity
@Table(name = "document_sequence", uniqueConstraints =
        @UniqueConstraint(columnNames = {"docType", "yearKey", "companyCode", "plantCode"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String docType; // e.g. "PO", "PR", "GRN"

    @Column(nullable = false)
    private String yearKey; // e.g. "2026"

    @Builder.Default
    @Column(nullable = false)
    private String companyCode = "DEFAULT"; // ready for multi-company, single company today

    @Builder.Default
    @Column(nullable = false)
    private String plantCode = "DEFAULT"; // ready for multi-plant, single plant today

    @Column(nullable = false)
    private long nextNumber;

    private String prefix;
    private String suffix;

    @Builder.Default
    @Column(nullable = false)
    private int padding = 6;
}
