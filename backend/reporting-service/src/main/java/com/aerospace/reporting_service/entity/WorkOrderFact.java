package com.aerospace.reporting_service.entity;

import java.time.LocalDateTime;

import org.springframework.cglib.core.Local;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "work_order_fact")
@Data
public class WorkOrderFact {
    @Id
    private Long workOrderId;
    private String woNumber;
    private String aircraftId;
    private String taskDescription;
    private String priority;
    private String status;
    private String createdBy;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer daysToComplete;
    
}
