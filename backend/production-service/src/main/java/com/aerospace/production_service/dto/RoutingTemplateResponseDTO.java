package com.aerospace.production_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingTemplateResponseDTO {
    private Long id;
    private String partNumber;
    private String name;
    private String description;
    private Boolean isActive;
    private Integer revisionNumber;
    private Long supersedesId;
    private Boolean isCurrentRevision;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RoutingOperationDTO> operations;
}
