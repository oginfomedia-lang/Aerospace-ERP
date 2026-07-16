package com.aerospace.production_service.mapper;

import com.aerospace.production_service.dto.RoutingOperationDTO;
import com.aerospace.production_service.dto.RoutingTemplateResponseDTO;
import com.aerospace.production_service.entity.RoutingOperation;
import com.aerospace.production_service.entity.RoutingTemplate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class RoutingTemplateMapper {

    public RoutingTemplateResponseDTO toDTO(RoutingTemplate template) {
        if (template == null) return null;

        List<RoutingOperationDTO> ops = template.getOperations().stream()
                .sorted(Comparator.comparing(RoutingOperation::getSequenceNumber))
                .map(this::toOperationDTO)
                .toList();

        return RoutingTemplateResponseDTO.builder()
                .id(template.getId())
                .partNumber(template.getPartNumber())
                .name(template.getName())
                .description(template.getDescription())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .operations(ops)
                .build();
    }

    public RoutingOperationDTO toOperationDTO(RoutingOperation op) {
        return RoutingOperationDTO.builder()
                .id(op.getId())
                .sequenceNumber(op.getSequenceNumber())
                .operationName(op.getOperationName())
                .operationType(op.getOperationType())
                .operationCode(op.getOperationCode())
                .expectedDurationMinutes(op.getExpectedDurationMinutes())
                .workstation(op.getWorkstation())
                .instructions(op.getInstructions())
                .build();
    }
}
