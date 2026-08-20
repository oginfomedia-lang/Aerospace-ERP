package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tax_jurisdictions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxJurisdiction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String jurisdictionCode; // e.g. "IN-KA", "IN-INTERSTATE"

    @Column(nullable = false, length = 100)
    private String jurisdictionName;

    @Builder.Default
    private Boolean activeStatus = true;
}
