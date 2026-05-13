package com.aerospace.production_service.dto;

import com.aerospace.production_service.entity.ProductionStageLog.StageStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StageLogRequestDTO {

    @NotBlank(message = "Stage name is required")
    private String stageName;

    private String stageCode;

    @NotNull(message = "Stage status is required")
    private StageStatus status;

    private Integer quantityIn;
    private Integer quantityOut;
    private Integer scrapCount;

    private String operatorName;
    private String remarks;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
