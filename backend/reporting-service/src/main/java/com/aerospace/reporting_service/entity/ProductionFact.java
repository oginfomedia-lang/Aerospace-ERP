package com.aerospace.reporting_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_fact")
@Data
public class ProductionFact {

    @Id
    private Long productionOrderId;
    private String poNumber;
    private String productName;
    private String productCode;
    private Integer plannedQuantity;
    private Integer producedQuantity;
    private Integer rejectedQuantity;
    private String priority;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Boolean qualityPassed;
}