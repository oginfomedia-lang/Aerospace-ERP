package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.SkillRequestDTO;
import com.aerospace.production_resource_service.dto.SkillResponseDTO;

import java.util.List;

public interface SkillService {
    SkillResponseDTO createSkill(SkillRequestDTO dto);
    List<SkillResponseDTO> listSkills();
    SkillResponseDTO getSkillById(Long id);
}
