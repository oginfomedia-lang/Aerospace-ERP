package com.aerospace.production_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingTemplateRequestDTO {
    @NotBlank
    private String partNumber;
    @NotBlank
    private String name;
    private String description;
    private Boolean isActive;
    private List<RoutingOperationDTO> operations;
}
