package com.aerospace.production_service.dto;

import com.aerospace.production_service.entity.ShopFloorDisposition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopFloorNcrResponseDTO {
    private Long id;
    private Long productionOrderId;
    private Long productionMaterialId;
    private String partName;
    private Long stageLogId;
    private String stageName;
    private Integer rejectedQuantity;
    private ShopFloorDisposition disposition;
    private String reasonCode;
    private String description;
    private String reportedBy;
    private LocalDateTime reportedAt;
    private Long reworkStageLogId;
}
