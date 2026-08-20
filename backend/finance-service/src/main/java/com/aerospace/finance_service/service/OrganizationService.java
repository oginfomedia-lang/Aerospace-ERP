package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.OrganizationDTO;

import java.util.List;

public interface OrganizationService {
    OrganizationDTO create(OrganizationDTO dto);
    OrganizationDTO getById(Long id);
    List<OrganizationDTO> getAll();
    OrganizationDTO update(Long id, OrganizationDTO dto);
    void deactivate(Long id);
}
