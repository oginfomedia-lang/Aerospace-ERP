package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.OrganizationDTO;
import com.aerospace.finance_service.entity.Organization;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.OrganizationRepository;
import com.aerospace.finance_service.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public OrganizationDTO create(OrganizationDTO dto) {
        if (organizationRepository.existsByOrgCode(dto.getOrgCode())) {
            throw new IllegalArgumentException("Organization code already exists: " + dto.getOrgCode());
        }
        Organization entity = Organization.builder()
                .orgCode(dto.getOrgCode())
                .orgName(dto.getOrgName())
                .baseCurrency(dto.getBaseCurrency())
                .activeStatus(dto.getActiveStatus() == null || dto.getActiveStatus())
                .build();
        return toDTO(organizationRepository.save(entity));
    }

    @Override
    public OrganizationDTO getById(Long id) {
        return toDTO(organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id)));
    }

    @Override
    public List<OrganizationDTO> getAll() {
        return organizationRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public OrganizationDTO update(Long id, OrganizationDTO dto) {
        Organization entity = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
        entity.setOrgName(dto.getOrgName());
        entity.setBaseCurrency(dto.getBaseCurrency());
        if (dto.getActiveStatus() != null) entity.setActiveStatus(dto.getActiveStatus());
        return toDTO(organizationRepository.save(entity));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Organization entity = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
        entity.setActiveStatus(false);
        organizationRepository.save(entity);
    }

    private OrganizationDTO toDTO(Organization e) {
        return OrganizationDTO.builder()
                .id(e.getId()).orgCode(e.getOrgCode()).orgName(e.getOrgName())
                .baseCurrency(e.getBaseCurrency()).activeStatus(e.getActiveStatus())
                .build();
    }
}
