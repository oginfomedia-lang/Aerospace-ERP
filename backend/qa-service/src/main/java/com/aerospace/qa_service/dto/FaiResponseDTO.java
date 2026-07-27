package com.aerospace.qa_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FaiResponseDTO {
    private Long id;
    private String faiNumber;
    private Long partId;
    private String partName;
    private String skuCode;
    private String partRevision;
    private String batchNumber;
    private String status;
    private String overallResult;
    private String materialCertRef;
    private String inspectorName;
    private String equipmentAssetTag;
    private LocalDateTime performedAt;
    private String remarks;
    private String signatureHash;
    private LocalDateTime createdAt;
    private List<FaiCharacteristicDTO> characteristics;
}
