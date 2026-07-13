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
}
