package com.aerospace.production_resource_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SkillUpgradeResponseDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long skillId;
    private String skillName;
    private Integer fromLevel;
    private Integer toLevel;
    private String status;
    private String reason;
    private String requestedBy;
    private LocalDateTime createdAt;
    private String decidedBy;
    private LocalDateTime decidedAt;
}
