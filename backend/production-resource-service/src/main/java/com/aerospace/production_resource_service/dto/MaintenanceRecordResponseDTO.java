package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MaintenanceRecordResponseDTO {
    private Long id;
    private Long machineId;
    private String machineCode;
    private String maintenanceType;
    private LocalDateTime performedAt;
    private String performedBy;
    private BigDecimal cost;
    private Integer downtimeMinutes;
    private String result;
    private String remarks;
    private LocalDateTime nextDueAt;
    private LocalDateTime createdAt;
}
