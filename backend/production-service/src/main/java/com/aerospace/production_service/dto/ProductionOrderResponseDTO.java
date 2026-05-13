package com.aerospace.production_service.dto;

import com.aerospace.production_service.entity.ProductionOrderStatus;
import com.aerospace.production_service.entity.ProductionPriority;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderResponseDTO {

    private Long id;
    private String poNumber;
    private String woNumber;
    private Long workOrderId;

    private String productName;
    private String productCode;
    private String aircraftType;

    private Integer plannedQuantity;
    private Integer producedQuantity;
    private Integer rejectedQuantity;

    private ProductionOrderStatus status;
    private ProductionPriority priority;

    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;

    private String productionLine;
    private String assignedTeam;
    private String supervisorName;
    private String createdBy;

    private String qualityStandard;
    private String qualityNotes;
    private Boolean qualityPassed;

    private String batchNumber;
    private String remarks;
    private String closureNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed field — for dashboard/progress display
    private Double completionPercentage;

    private List<ProductionMaterialDTO> materials;
    private List<StageLogResponseDTO> stageLogs;
}
