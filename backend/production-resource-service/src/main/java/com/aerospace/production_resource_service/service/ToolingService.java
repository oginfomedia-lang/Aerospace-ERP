package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.ToolingRequestDTO;
import com.aerospace.production_resource_service.dto.ToolingResponseDTO;

import java.util.List;

public interface ToolingService {
    ToolingResponseDTO registerTooling(ToolingRequestDTO dto);
    ToolingResponseDTO updateTooling(Long id, ToolingRequestDTO dto);
    List<ToolingResponseDTO> listTooling(String status, String toolingType);
    ToolingResponseDTO getToolingById(Long id);
    void deleteTooling(Long id);
    ToolingResponseDTO occupyTooling(Long id, Long stageLogId);
    ToolingResponseDTO releaseTooling(Long id);
}
