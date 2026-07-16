package com.aerospace.production_service.mapper;

import com.aerospace.production_service.dto.ShopFloorNcrResponseDTO;
import com.aerospace.production_service.entity.ShopFloorNcr;
import org.springframework.stereotype.Component;

@Component
public class ShopFloorNcrMapper {

    public ShopFloorNcrResponseDTO toDTO(ShopFloorNcr ncr) {
        if (ncr == null) return null;
        return ShopFloorNcrResponseDTO.builder()
                .id(ncr.getId())
                .productionOrderId(ncr.getProductionOrderId())
                .productionMaterialId(ncr.getProductionMaterial() != null ? ncr.getProductionMaterial().getId() : null)
                .partName(ncr.getProductionMaterial() != null ? ncr.getProductionMaterial().getPartName() : null)
                .stageLogId(ncr.getStageLog() != null ? ncr.getStageLog().getId() : null)
                .stageName(ncr.getStageLog() != null ? ncr.getStageLog().getStageName() : null)
                .rejectedQuantity(ncr.getRejectedQuantity())
                .disposition(ncr.getDisposition())
                .reasonCode(ncr.getReasonCode())
                .description(ncr.getDescription())
                .reportedBy(ncr.getReportedBy())
                .reportedAt(ncr.getReportedAt())
                .reworkStageLogId(ncr.getReworkStageLogId())
                .build();
    }
}
