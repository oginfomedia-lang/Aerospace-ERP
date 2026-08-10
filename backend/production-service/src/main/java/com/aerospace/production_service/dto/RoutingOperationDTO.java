package com.aerospace.production_service.dto;

import com.aerospace.production_service.entity.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Used both as request (create/update) and response shape for a routing operation line.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingOperationDTO {
    private Long id;
    private Integer sequenceNumber;
    private String operationName;
    private OperationType operationType;
    private String operationCode;
    private Integer expectedDurationMinutes;
    private String workstation;
    private String instructions;

    private Long preferredMachineId;
    private String preferredMachineCode;
    private Long preferredEmployeeId;
    private String preferredEmployeeName;
}
