package com.aerospace.inventory_service.mapper;

import com.aerospace.inventory_service.dto.*;
import com.aerospace.inventory_service.entity.Part;
import org.springframework.stereotype.Component;

@Component
public class PartMapper {

    public Part toEntity(PartRequestDTO dto) {
        if (dto == null) return null;

        Part.PartBuilder builder = Part.builder()
                .name(dto.getName())
                // serialNumber from DTO maps to SKU/Code in ERP Entity
                .skuCode(dto.getSerialNumber())
                .partType(dto.getPartType())
                .criticality(dto.getCriticality())
                .reorderLevel(dto.getReorderLevel())
                .minStock(dto.getMinStock())
                .maxStock(dto.getMaxStock())
                .safetyStock(dto.getSafetyStock())
                .leadTimeDays(dto.getLeadTimeDays())
                .shelfLifeDays(dto.getShelfLifeDays())
                .preferredSupplierId(dto.getPreferredSupplierId());
                // Removed quantity, batch, and status as they are now Ledger-based

        if (dto.getItemStatus() != null) builder.status(dto.getItemStatus());
        if (dto.getBatchRequired() != null) builder.batchRequired(dto.getBatchRequired());
        if (dto.getSerialRequired() != null) builder.serialRequired(dto.getSerialRequired());
        if (dto.getExpiryTracking() != null) builder.expiryTracking(dto.getExpiryTracking());
        if (dto.getQcRequired() != null) builder.qcRequired(dto.getQcRequired());

        return builder.build();
    }

    public PartResponseDTO toDTO(Part part) {
        if (part == null) return null;

        return PartResponseDTO.builder()
                .id(part.getId())
                .name(part.getName())
                .serialNumber(part.getSkuCode())
                .partType(part.getPartType())
                .criticality(part.getCriticality())
                .reorderLevel(part.getReorderLevel())
                .minStock(part.getMinStock())
                .maxStock(part.getMaxStock())
                .safetyStock(part.getSafetyStock())
                .leadTimeDays(part.getLeadTimeDays())
                .itemStatus(part.getStatus())
                .batchRequired(part.isBatchRequired())
                .serialRequired(part.isSerialRequired())
                .expiryTracking(part.isExpiryTracking())
                .shelfLifeDays(part.getShelfLifeDays())
                .qcRequired(part.isQcRequired())
                .preferredSupplierId(part.getPreferredSupplierId())
                // Note: quantity, status (stock bucket), and batchNumber will be set
                // by the Service layer after aggregating StockBalances
                .build();
    }
}
