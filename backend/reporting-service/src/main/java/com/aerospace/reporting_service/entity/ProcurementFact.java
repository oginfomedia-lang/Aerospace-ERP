package com.aerospace.reporting_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "procurement_fact")
@Data
public class ProcurementFact {

    @Id
    private Long purchaseOrderId;
    private String poNumber;
    private String supplierName;
    private Double totalAmount;
    private String currency;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private String status;
}