package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates", uniqueConstraints = @UniqueConstraint(columnNames = {"fromCurrency", "toCurrency", "effectiveDate"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String fromCurrency;
    @Column(nullable = false, length = 3)
    private String toCurrency;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal rate; // 1 fromCurrency = `rate` toCurrency

    @Column(nullable = false)
    private LocalDate effectiveDate; // history is simply "one row per date"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
