package com.aerospace.production_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

// Local mirror of production-resource-service's MachineReservationResponseDTO /
// EmployeeReservationResponseDTO — only the fields needed to build a clear rejection
// message when startStage finds a conflicting advance booking.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservationConflictInfo {
    private LocalDateTime reservedFrom;
    private LocalDateTime reservedTo;
    private String createdBy;
}
