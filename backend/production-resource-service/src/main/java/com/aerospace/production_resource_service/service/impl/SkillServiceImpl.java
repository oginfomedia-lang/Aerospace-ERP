package com.aerospace.production_resource_service.service.impl;

import com.aerospace.production_resource_service.dto.SkillRequestDTO;
import com.aerospace.production_resource_service.dto.SkillResponseDTO;
import com.aerospace.production_resource_service.entity.Skill;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.SkillRepository;
import com.aerospace.production_resource_service.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;

    @Override
    @Transactional
    public SkillResponseDTO createSkill(SkillRequestDTO dto) {
        if (skillRepository.existsByName(dto.getName())) {
            throw new IllegalStateException("A skill named '" + dto.getName() + "' already exists");
        }
        Skill skill = Skill.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        Skill saved = skillRepository.save(skill);
        log.info("Skill created — name: {}", saved.getName());
        return toDTO(saved);
    }

    @Override
    public List<SkillResponseDTO> listSkills() {
        return skillRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public SkillResponseDTO getSkillById(Long id) {
        return toDTO(findOrThrow(id));
    }

    private Skill findOrThrow(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + id));
    }

    private SkillResponseDTO toDTO(Skill s) {
        return SkillResponseDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .build();
    }
}
