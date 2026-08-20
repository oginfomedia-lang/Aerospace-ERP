package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "currencies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Currency {

    @Id
    @Column(length = 3)
    private String currencyCode; // ISO 4217, e.g. "INR", "USD"

    @Column(nullable = false, length = 60)
    private String currencyName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isBaseCurrency = false; // exactly one row should be true

    @Column(nullable = false)
    @Builder.Default
    private Boolean activeStatus = true;
}
