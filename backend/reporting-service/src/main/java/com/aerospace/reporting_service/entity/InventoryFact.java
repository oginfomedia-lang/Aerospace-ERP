package com.aerospace.reporting_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_fact")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer quantity;
    private String status;
    private String location;
    private LocalDateTime lastUpdated;

    private Double unitPrice; //for inventory value tracking
}
