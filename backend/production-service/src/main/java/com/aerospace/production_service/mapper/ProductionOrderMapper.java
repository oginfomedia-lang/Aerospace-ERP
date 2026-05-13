package com.aerospace.production_service.mapper;

import com.aerospace.production_service.dto.*;
import com.aerospace.production_service.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductionOrderMapper {

    public ProductionOrder toEntity(ProductionOrderRequestDTO dto) {
        if (dto == null) return null;
        return ProductionOrder.builder()
                .productName(dto.getProductName())
                .productCode(dto.getProductCode())
                .aircraftType(dto.getAircraftType())
                .woNumber(dto.getWoNumber())
                .workOrderId(dto.getWorkOrderId())
                .plannedQuantity(dto.getPlannedQuantity())
                .priority(dto.getPriority())
                .plannedStartDate(dto.getPlannedStartDate())
                .plannedEndDate(dto.getPlannedEndDate())
                .productionLine(dto.getProductionLine())
                .assignedTeam(dto.getAssignedTeam())
                .supervisorName(dto.getSupervisorName())
                .createdBy(dto.getCreatedBy())
                .qualityStandard(dto.getQualityStandard())
                .qualityNotes(dto.getQualityNotes())
                .batchNumber(dto.getBatchNumber())
                .remarks(dto.getRemarks())
                .status(ProductionOrderStatus.PLANNED)
                .build();
    }

    public ProductionOrderResponseDTO toDTO(ProductionOrder order) {
        if (order == null) return null;

        double completion = 0.0;
        if (order.getPlannedQuantity() != null && order.getPlannedQuantity() > 0
                && order.getProducedQuantity() != null) {
            completion = (order.getProducedQuantity() * 100.0) / order.getPlannedQuantity();
        }

        return ProductionOrderResponseDTO.builder()
                .id(order.getId())
                .poNumber(order.getPoNumber())
                .woNumber(order.getWoNumber())
                .workOrderId(order.getWorkOrderId())
                .productName(order.getProductName())
                .productCode(order.getProductCode())
                .aircraftType(order.getAircraftType())
                .plannedQuantity(order.getPlannedQuantity())
                .producedQuantity(order.getProducedQuantity())
                .rejectedQuantity(order.getRejectedQuantity())
                .status(order.getStatus())
                .priority(order.getPriority())
                .plannedStartDate(order.getPlannedStartDate())
                .plannedEndDate(order.getPlannedEndDate())
                .actualStartDate(order.getActualStartDate())
                .actualEndDate(order.getActualEndDate())
                .productionLine(order.getProductionLine())
                .assignedTeam(order.getAssignedTeam())
                .supervisorName(order.getSupervisorName())
                .createdBy(order.getCreatedBy())
                .qualityStandard(order.getQualityStandard())
                .qualityNotes(order.getQualityNotes())
                .qualityPassed(order.getQualityPassed())
                .batchNumber(order.getBatchNumber())
                .remarks(order.getRemarks())
                .closureNotes(order.getClosureNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completionPercentage(Math.min(completion, 100.0))
                .materials(mapMaterials(order.getMaterials()))
                .stageLogs(mapStageLogs(order.getStageLogs()))
                .build();
    }

    public ProductionMaterialDTO toMaterialDTO(ProductionMaterial m) {
        if (m == null) return null;
        return ProductionMaterialDTO.builder()
                .id(m.getId())
                .partId(m.getPartId())
                .partName(m.getPartName())
                .partNumber(m.getPartNumber())
                .batchNumber(m.getBatchNumber())
                .quantityRequired(m.getQuantityRequired())
                .quantityConsumed(m.getQuantityConsumed())
                .quantityWasted(m.getQuantityWasted())
                .status(m.getStatus())
                .remarks(m.getRemarks())
                .build();
    }

    public StageLogResponseDTO toStageLogDTO(ProductionStageLog log) {
        if (log == null) return null;
        return StageLogResponseDTO.builder()
                .id(log.getId())
                .stageName(log.getStageName())
                .stageCode(log.getStageCode())
                .status(log.getStatus())
                .quantityIn(log.getQuantityIn())
                .quantityOut(log.getQuantityOut())
                .scrapCount(log.getScrapCount())
                .operatorName(log.getOperatorName())
                .remarks(log.getRemarks())
                .startedAt(log.getStartedAt())
                .completedAt(log.getCompletedAt())
                .loggedAt(log.getLoggedAt())
                .build();
    }

    private List<ProductionMaterialDTO> mapMaterials(List<ProductionMaterial> materials) {
        if (materials == null) return Collections.emptyList();
        return materials.stream().map(this::toMaterialDTO).toList();
    }

    private List<StageLogResponseDTO> mapStageLogs(List<ProductionStageLog> logs) {
        if (logs == null) return Collections.emptyList();
        return logs.stream().map(this::toStageLogDTO).toList();
    }
}
