package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeLeaveResponseDTO {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String status;
    private String decidedBy;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}
