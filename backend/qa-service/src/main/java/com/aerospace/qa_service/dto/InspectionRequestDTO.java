package com.aerospace.qa_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// What the QA engineer submits via the API
@Data
public class InspectionRequestDTO {

    @NotNull(message = "Part ID is required")
    private Long partId;

    @NotBlank(message = "Batch number is required")
    private String batchNumber;

    @NotNull(message = "Approved quantity is required (0 if none)")
    @Min(value = 0, message = "Approved quantity cannot be negative")
    private Integer approvedQty;

    @NotNull(message = "Rejected quantity is required (0 if none)")
    @Min(value = 0, message = "Rejected quantity cannot be negative")
    private Integer rejectedQty;

    // The batch's total pending quantity at inspection time — the caller already knows this
    // (it's what's shown in the QA queue), sent through so the service can validate
    // approvedQty + rejectedQty == totalQuantity synchronously instead of failing later,
    // silently, inside the async Kafka consumer on the inventory-service side.
    @NotNull(message = "Total quantity is required to validate the approved/rejected split")
    private Integer totalQuantity;

    // The acceptance standard/spec applied for this inspection.
    private String criteria;

    @NotBlank(message = "Remarks are mandatory for aerospace traceability")
    private String remarks;

    // Optional fields
    private String certificateNumber;  // e.g. "CERT-2024-00123"
    private String inspectorName;
    private String location;           // needed so inventory knows which bin to update
}