package com.aerospace.production_resource_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.production_resource_service.common.ApiResponse;
import com.aerospace.production_resource_service.dto.SkillRequestDTO;
import com.aerospace.production_resource_service.dto.SkillResponseDTO;
import com.aerospace.production_resource_service.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources/skills")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','MANAGER','ADMIN')")
public class SkillController {

    private final SkillService skillService;

    @Audited(action = "CREATE", entityType = "Skill")
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<SkillResponseDTO>> createSkill(@Valid @RequestBody SkillRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SKILL_CREATED", skillService.createSkill(dto)));
    }

    // PRODUCTION_ENGINEER needs read access to populate the skill-filter picker in
    // the Routing Template editor, without gaining any write access to the catalog.
    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillResponseDTO>>> listSkills() {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", skillService.listSkills()));
    }

    @PreAuthorize("hasAnyRole('PRODUCTION_SUPERVISOR','MAINTENANCE_ENGINEER','PRODUCTION_ENGINEER','MANAGER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillResponseDTO>> getSkill(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", skillService.getSkillById(id)));
    }
}
