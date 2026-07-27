package com.aerospace.qa_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FaiPerformRequestDTO {
    private String batchNumber;
    private String partRevision;
    private String materialCertRef;
    private String inspectorName;
    private String equipmentAssetTag;

    @NotBlank(message = "Remarks are mandatory for aerospace traceability")
    private String remarks;

    @NotNull(message = "Overall result is required")
    private String overallResult; // CONFORMING, NON_CONFORMING

    @NotEmpty(message = "At least one characteristic is required")
    private List<FaiCharacteristicDTO> characteristics;
}
