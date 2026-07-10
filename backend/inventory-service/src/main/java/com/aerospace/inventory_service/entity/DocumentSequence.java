package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

// Central document numbering per ARC-BR-006: GRN numbers (and future doc types)
// are always system-generated, never free-typed by the caller.
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
    private String docType; // e.g. "GRN"

    @Column(nullable = false)
    private String yearKey; // e.g. "2026"

    @Column(nullable = false)
    private long nextNumber;
}
