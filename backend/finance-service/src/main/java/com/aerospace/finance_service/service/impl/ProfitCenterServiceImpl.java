package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.ProfitCenterDTO;
import com.aerospace.finance_service.entity.ProfitCenter;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.ProfitCenterRepository;
import com.aerospace.finance_service.service.ProfitCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfitCenterServiceImpl implements ProfitCenterService {

    private final ProfitCenterRepository profitCenterRepository;

    @Override
    @Transactional
    public ProfitCenterDTO create(ProfitCenterDTO dto) {
        if (profitCenterRepository.existsByProfitCenterCode(dto.getProfitCenterCode())) {
            throw new IllegalArgumentException("Profit center code already exists: " + dto.getProfitCenterCode());
        }
        ProfitCenter entity = ProfitCenter.builder()
                .profitCenterCode(dto.getProfitCenterCode())
                .profitCenterName(dto.getProfitCenterName())
                .plantId(dto.getPlantId())
                .activeStatus(dto.getActiveStatus() == null || dto.getActiveStatus())
                .build();
        return toDTO(profitCenterRepository.save(entity));
    }

    @Override
    public ProfitCenterDTO getById(Long id) {
        return toDTO(profitCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profit center not found: " + id)));
    }

    @Override
    public List<ProfitCenterDTO> getAll() {
        return profitCenterRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public ProfitCenterDTO update(Long id, ProfitCenterDTO dto) {
        ProfitCenter entity = profitCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profit center not found: " + id));
        entity.setProfitCenterName(dto.getProfitCenterName());
        entity.setPlantId(dto.getPlantId());
        if (dto.getActiveStatus() != null) entity.setActiveStatus(dto.getActiveStatus());
        return toDTO(profitCenterRepository.save(entity));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        ProfitCenter entity = profitCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profit center not found: " + id));
        entity.setActiveStatus(false);
        profitCenterRepository.save(entity);
    }

    private ProfitCenterDTO toDTO(ProfitCenter e) {
        return ProfitCenterDTO.builder()
                .id(e.getId()).profitCenterCode(e.getProfitCenterCode()).profitCenterName(e.getProfitCenterName())
                .plantId(e.getPlantId()).activeStatus(e.getActiveStatus())
                .build();
    }
}
