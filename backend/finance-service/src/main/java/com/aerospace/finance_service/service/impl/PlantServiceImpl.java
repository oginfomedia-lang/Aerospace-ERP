package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.PlantDTO;
import com.aerospace.finance_service.entity.Plant;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.PlantRepository;
import com.aerospace.finance_service.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlantServiceImpl implements PlantService {

    private final PlantRepository plantRepository;

    @Override
    @Transactional
    public PlantDTO create(PlantDTO dto) {
        if (plantRepository.existsByPlantCode(dto.getPlantCode())) {
            throw new IllegalArgumentException("Plant code already exists: " + dto.getPlantCode());
        }
        Plant entity = Plant.builder()
                .plantCode(dto.getPlantCode())
                .plantName(dto.getPlantName())
                .organizationId(dto.getOrganizationId())
                .activeStatus(dto.getActiveStatus() == null || dto.getActiveStatus())
                .build();
        return toDTO(plantRepository.save(entity));
    }

    @Override
    public PlantDTO getById(Long id) {
        return toDTO(plantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plant not found: " + id)));
    }

    @Override
    public List<PlantDTO> getAll() {
        return plantRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public PlantDTO update(Long id, PlantDTO dto) {
        Plant entity = plantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plant not found: " + id));
        entity.setPlantName(dto.getPlantName());
        entity.setOrganizationId(dto.getOrganizationId());
        if (dto.getActiveStatus() != null) entity.setActiveStatus(dto.getActiveStatus());
        return toDTO(plantRepository.save(entity));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Plant entity = plantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plant not found: " + id));
        entity.setActiveStatus(false);
        plantRepository.save(entity);
    }

    private PlantDTO toDTO(Plant e) {
        return PlantDTO.builder()
                .id(e.getId()).plantCode(e.getPlantCode()).plantName(e.getPlantName())
                .organizationId(e.getOrganizationId()).activeStatus(e.getActiveStatus())
                .build();
    }
}
