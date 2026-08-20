package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tax_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String categoryCode; // e.g. "GST_STANDARD", "GST_EXEMPT"

    @Column(nullable = false, length = 100)
    private String categoryName;

    @Builder.Default
    private Boolean activeStatus = true;
}
