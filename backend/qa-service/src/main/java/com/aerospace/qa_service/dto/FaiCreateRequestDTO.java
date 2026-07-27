package com.aerospace.qa_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FaiCreateRequestDTO {
    @NotNull(message = "Part ID is required")
    private Long partId;
    private String partName;
    private String skuCode;
}
