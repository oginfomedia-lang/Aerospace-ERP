package com.aerospace.production_service.dto;

import com.aerospace.production_service.entity.ProductionOrderStatus;
import com.aerospace.production_service.entity.ProductionPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductionOrderRequestDTO {

    @NotBlank(message = "Product name is required")
    private String productName;

    private String productCode;
    private String aircraftType;
    private String woNumber;
    private Long workOrderId;

    @NotNull(message = "Planned quantity is required")
    @Min(value = 1, message = "Planned quantity must be at least 1")
    private Integer plannedQuantity;

    @NotNull(message = "Priority is required")
    private ProductionPriority priority;

    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;

    private String productionLine;
    private String assignedTeam;
    private String supervisorName;
    private String createdBy;

    private String qualityStandard;
    private String qualityNotes;
    private String batchNumber;
    private String remarks;

    // Materials to be consumed — provided at creation time (Bill of Materials)
    private List<ProductionMaterialDTO> materials;
}
