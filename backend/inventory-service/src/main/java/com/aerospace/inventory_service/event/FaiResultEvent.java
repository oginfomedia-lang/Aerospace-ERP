package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaiResultEvent {
    private Long id;
    private String faiNumber;
    private Long partId;
    private String partName;
    private String skuCode;
    private String batchNumber;
    private String partRevision;
    private String overallResult; // "CONFORMING" or "NON_CONFORMING"
    private String inspectorName;
    private String equipmentAssetTag;
    private String remarks;
    private LocalDateTime performedAt;
}
