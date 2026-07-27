package com.aerospace.qa_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CocResponse {
    private Long id;
    private String cocNumber;
    private Long partId;
    private String partName;
    private String skuCode;
    private String batchNumber;
    private String poNumber;
    private Integer quantityCertified;
    private Long inspectionReportId;
    private String certifyingStandard;
    private String issuedBy;
    private LocalDateTime issuedAt;
    private String signatureHash;
}
