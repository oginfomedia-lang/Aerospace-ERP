package com.aerospace.qa_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CalibrationRecordResponseDTO {
    private Long id;
    private Long equipmentId;
    private String assetTag;
    private LocalDateTime calibratedAt;
    private String performedBy;
    private String certificateNumber;
    private String result;
    private String remarks;
    private LocalDateTime nextDueAt;
    private LocalDateTime createdAt;
}
