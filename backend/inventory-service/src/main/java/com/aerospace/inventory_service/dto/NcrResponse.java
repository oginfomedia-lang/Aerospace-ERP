package com.aerospace.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NcrResponse {
    private Long id;
    private String ncrNumber;
    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer quantity;
    private String reason;
    private String status;
    private String dispositionType;
    private String dispositionRemarks;
    private String dispositionedBy;
    private LocalDateTime createdAt;
    private LocalDateTime dispositionedAt;
    // True when a non-terminal (not CLOSED/REJECTED) CAPA already exists against this NCR —
    // lets the frontend show "CAPA Already Raised" instead of a clickable Open CAPA button.
    private boolean hasActiveCapa;
}
