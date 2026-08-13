package com.aerospace.sales_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InspectReturnRequestDTO {
    // ACCEPTED, REJECTED, REPAIR, REPLACE, or CREDIT_RECOMMENDED — parsed against
    // ReturnDisposition in the service layer, so a bad value fails with a clear 400
    // rather than a Jackson deserialization error.
    @NotBlank(message = "Disposition is required")
    private String disposition;

    private String notes;
}
