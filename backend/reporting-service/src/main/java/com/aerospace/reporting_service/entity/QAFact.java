package com.aerospace.reporting_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "qa_fact")
@Data
public class QAFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long partId;
    private String partName;
    private String batchNumber;
    private String decision;      // APPROVED / REJECTED
    private String inspectorName;
    private LocalDateTime inspectedAt;
}