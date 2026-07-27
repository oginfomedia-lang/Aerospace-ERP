package com.aerospace.qa_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

// What's submitted when recording a calibration event against an existing equipment item
@Data
public class CalibrationRequestDTO {

    private LocalDateTime calibratedAt; // defaults to now() if omitted

    private String performedBy;
    private String certificateNumber;

    @NotNull(message = "Result is required")
    private String result; // PASS, FAIL, ADJUSTED

    @NotBlank(message = "Remarks are mandatory for aerospace traceability")
    private String remarks;
}
