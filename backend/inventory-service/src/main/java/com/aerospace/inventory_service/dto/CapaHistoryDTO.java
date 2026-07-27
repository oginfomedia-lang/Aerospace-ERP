package com.aerospace.inventory_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CapaHistoryDTO {
    private String fromStatus;
    private String toStatus;
    private String actor;
    private String remarks;
    private String signatureHash;
    private LocalDateTime occurredAt;
}
