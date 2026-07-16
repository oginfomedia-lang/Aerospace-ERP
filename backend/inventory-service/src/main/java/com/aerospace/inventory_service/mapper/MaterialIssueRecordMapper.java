package com.aerospace.inventory_service.mapper;

import com.aerospace.inventory_service.dto.MaterialIssueRecordLineResponse;
import com.aerospace.inventory_service.dto.MaterialIssueRecordResponse;
import com.aerospace.inventory_service.entity.MaterialIssueRecord;
import com.aerospace.inventory_service.entity.MaterialIssueRecordLine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MaterialIssueRecordMapper {

    public MaterialIssueRecordResponse toDTO(MaterialIssueRecord record) {
        if (record == null) return null;

        return MaterialIssueRecordResponse.builder()
                .id(record.getId())
                .issueNumber(record.getIssueNumber())
                .partId(record.getPartId())
                .partName(record.getPartName())
                .totalQuantityIssued(record.getTotalQuantityIssued())
                .workOrderId(record.getWorkOrderId())
                .woNumber(record.getWoNumber())
                .lineItemId(record.getLineItemId())
                .issuedTo(record.getIssuedTo())
                .issuedBy(record.getIssuedBy())
                .issuedAt(record.getIssuedAt())
                .lines(record.getLines() != null
                        ? record.getLines().stream().map(this::toLineDTO).collect(Collectors.toList())
                        : List.of())
                .build();
    }

    private MaterialIssueRecordLineResponse toLineDTO(MaterialIssueRecordLine line) {
        return MaterialIssueRecordLineResponse.builder()
                .id(line.getId())
                .batchNumber(line.getBatchNumber())
                .quantityTaken(line.getQuantityTaken())
                .location(line.getLocation())
                .expiryDate(line.getExpiryDate())
                .build();
    }
}
