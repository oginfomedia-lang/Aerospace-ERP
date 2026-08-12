package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.SkillUpgradeRequestCreateDTO;
import com.aerospace.production_resource_service.dto.SkillUpgradeResponseDTO;

import java.util.List;

public interface SkillUpgradeService {
    SkillUpgradeResponseDTO requestUpgrade(Long employeeId, SkillUpgradeRequestCreateDTO dto, String requestedBy);
    List<SkillUpgradeResponseDTO> getHistory(Long employeeId);
    List<SkillUpgradeResponseDTO> getPending();
    SkillUpgradeResponseDTO approve(Long employeeId, Long requestId, String decidedBy);
    SkillUpgradeResponseDTO reject(Long employeeId, Long requestId, String decidedBy);
}
