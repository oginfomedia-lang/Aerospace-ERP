package com.aerospace.qa_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CocGenerateRequest {
    @NotNull(message = "Inspection report ID is required")
    private Long inspectionReportId;

    // Caller-supplied context — InspectionReport itself only stores partId/batchNumber
    private String partName;
    private String skuCode;
    private String poNumber;
    private String certifyingStandard;

    @NotBlank(message = "Issuer name is required")
    private String issuedBy;
}
