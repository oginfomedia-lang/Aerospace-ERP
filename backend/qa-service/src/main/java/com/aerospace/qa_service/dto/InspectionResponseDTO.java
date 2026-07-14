package com.aerospace.qa_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

// What the API sends back after saving an inspection
@Data
@Builder
public class InspectionResponseDTO {
    private Long id;
    private Long partId;
    private String batchNumber;
    private String decision;           // APPROVED, REJECTED, or PARTIALLY_APPROVED
    private Integer approvedQty;
    private Integer rejectedQty;
    private String criteria;
    private String remarks;
    private String certificateNumber;
    private String inspectorName;
    private LocalDateTime inspectedAt;
    private boolean publishedToInventory;  // true once Kafka message sent successfully
}