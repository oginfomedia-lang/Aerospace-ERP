package com.aerospace.production_service.service;

import com.aerospace.production_service.dto.RoutingTemplateRequestDTO;
import com.aerospace.production_service.dto.RoutingTemplateResponseDTO;

import java.util.List;

public interface RoutingTemplateService {
    RoutingTemplateResponseDTO create(RoutingTemplateRequestDTO dto);
    RoutingTemplateResponseDTO update(Long id, RoutingTemplateRequestDTO dto);
    void delete(Long id);
    RoutingTemplateResponseDTO getById(Long id);
    List<RoutingTemplateResponseDTO> getAll();
    List<RoutingTemplateResponseDTO> getByPartNumber(String partNumber);
}
