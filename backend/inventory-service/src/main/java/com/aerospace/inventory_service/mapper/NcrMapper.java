package com.aerospace.inventory_service.mapper;

import com.aerospace.inventory_service.dto.NcrResponse;
import com.aerospace.inventory_service.entity.NonConformanceReport;
import org.springframework.stereotype.Component;

@Component
public class NcrMapper {

    public NcrResponse toDTO(NonConformanceReport ncr) {
        if (ncr == null) return null;

        return NcrResponse.builder()
                .id(ncr.getId())
                .ncrNumber(ncr.getNcrNumber())
                .partId(ncr.getPartId())
                .partName(ncr.getPartName())
                .batchNumber(ncr.getBatchNumber())
                .quantity(ncr.getQuantity())
                .reason(ncr.getReason())
                .status(ncr.getStatus().name())
                .dispositionType(ncr.getDispositionType() != null ? ncr.getDispositionType().name() : null)
                .dispositionRemarks(ncr.getDispositionRemarks())
                .dispositionedBy(ncr.getDispositionedBy())
                .createdAt(ncr.getCreatedAt())
                .dispositionedAt(ncr.getDispositionedAt())
                .build();
    }
}
