package com.aerospace.sales_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmDispatchRequestDTO {
    @NotBlank(message = "Carrier name is required")
    private String carrierName;

    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;
}
