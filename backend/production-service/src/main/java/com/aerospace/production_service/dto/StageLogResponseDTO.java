package com.aerospace.production_service.dto;

import com.aerospace.production_service.entity.OperationType;
import com.aerospace.production_service.entity.ProductionStageLog.StageStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageLogResponseDTO {

    private Long id;
    private Long productionMaterialId; // null for the order-level Final Assembly stage
    private Integer sequenceNumber;
    private Integer attemptNumber;
    private Long routingOperationId;
    private OperationType operationType;
    private String stageName;
    private String stageCode;
    private StageStatus status;
    private Integer quantityIn;
    private Integer quantityOut;
    private Integer scrapCount;
    private Long machineId;
    private Long employeeId;
    private Long toolingId;
    private String operatorName;
    private String remarks;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime loggedAt;
}
