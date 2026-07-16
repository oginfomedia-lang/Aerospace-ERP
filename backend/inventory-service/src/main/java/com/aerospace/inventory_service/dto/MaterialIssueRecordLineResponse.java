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
public class MaterialIssueRecordLineResponse {
    private Long id;
    private String batchNumber;
    private Integer quantityTaken;
    private String location;
    private LocalDateTime expiryDate;
}
