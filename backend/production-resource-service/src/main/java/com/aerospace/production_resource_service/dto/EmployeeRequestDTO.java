package com.aerospace.production_resource_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class EmployeeRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    private String designation;
    private String department;
    private String status; // ACTIVE, INACTIVE — ignored on create, always forced ACTIVE
    private List<Long> skillIds;
}
