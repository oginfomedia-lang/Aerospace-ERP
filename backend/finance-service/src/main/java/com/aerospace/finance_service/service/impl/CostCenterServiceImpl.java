package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.CostCenterDTO;
import com.aerospace.finance_service.entity.CostCenter;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.CostCenterRepository;
import com.aerospace.finance_service.service.CostCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CostCenterServiceImpl implements CostCenterService {

    private final CostCenterRepository costCenterRepository;

    @Override
    @Transactional
    public CostCenterDTO create(CostCenterDTO dto) {
        if (costCenterRepository.existsByCostCenterCode(dto.getCostCenterCode())) {
            throw new IllegalArgumentException("Cost center code already exists: " + dto.getCostCenterCode());
        }
        CostCenter entity = CostCenter.builder()
                .costCenterCode(dto.getCostCenterCode())
                .costCenterName(dto.getCostCenterName())
                .description(dto.getDescription())
                .plantId(dto.getPlantId())
                .activeStatus(dto.getActiveStatus() == null || dto.getActiveStatus())
                .build();
        return toDTO(costCenterRepository.save(entity));
    }

    @Override
    public CostCenterDTO getById(Long id) {
        return toDTO(costCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cost center not found: " + id)));
    }

    @Override
    public List<CostCenterDTO> getAll() {
        return costCenterRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public CostCenterDTO update(Long id, CostCenterDTO dto) {
        CostCenter entity = costCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cost center not found: " + id));
        entity.setCostCenterName(dto.getCostCenterName());
        entity.setDescription(dto.getDescription());
        entity.setPlantId(dto.getPlantId());
        if (dto.getActiveStatus() != null) entity.setActiveStatus(dto.getActiveStatus());
        return toDTO(costCenterRepository.save(entity));
    }

    // No hard delete exposed — master data referenced by journal lines is deactivated,
    // never deleted (same reasoning as Chart of Accounts).
    @Override
    @Transactional
    public void deactivate(Long id) {
        CostCenter entity = costCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cost center not found: " + id));
        entity.setActiveStatus(false);
        costCenterRepository.save(entity);
    }

    private CostCenterDTO toDTO(CostCenter e) {
        return CostCenterDTO.builder()
                .id(e.getId()).costCenterCode(e.getCostCenterCode()).costCenterName(e.getCostCenterName())
                .description(e.getDescription()).plantId(e.getPlantId()).activeStatus(e.getActiveStatus())
                .build();
    }
}
