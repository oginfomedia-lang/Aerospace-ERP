package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;

// Central document numbering per ARC-BR-006 — PO numbers are always system-generated.
@Entity
@Table(name = "document_sequence", uniqueConstraints =
        @UniqueConstraint(columnNames = {"docType", "yearKey"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String docType; // e.g. "PO"

    @Column(nullable = false)
    private String yearKey; // e.g. "2026"

    @Column(nullable = false)
    private long nextNumber;

    private String prefix;
    private String suffix;

    @Builder.Default
    @Column(nullable=false)
    private int padding = 5;
}
