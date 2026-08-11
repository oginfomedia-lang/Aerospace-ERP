package com.aerospace.production_resource_service.service.impl;

import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.production_resource_service.dto.ToolingRequestDTO;
import com.aerospace.production_resource_service.dto.ToolingResponseDTO;
import com.aerospace.production_resource_service.entity.Tooling;
import com.aerospace.production_resource_service.entity.ToolingOccupancy;
import com.aerospace.production_resource_service.entity.ToolingStatus;
import com.aerospace.production_resource_service.entity.ToolingType;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.ToolingRepository;
import com.aerospace.production_resource_service.service.ToolingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolingServiceImpl implements ToolingService {

    private final ToolingRepository toolingRepository;
    private final DocumentSequenceService documentSequenceService;

    @Override
    @Transactional
    public ToolingResponseDTO registerTooling(ToolingRequestDTO dto) {
        Tooling tooling = Tooling.builder()
                .toolCode(documentSequenceService.generate("TOOL"))
                .name(dto.getName())
                .toolingType(ToolingType.valueOf(dto.getToolingType()))
                .location(dto.getLocation())
                .status(ToolingStatus.ACTIVE)
                .build();

        Tooling saved = toolingRepository.save(tooling);
        log.info("Tooling registered — toolCode: {}, name: {}", saved.getToolCode(), saved.getName());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public ToolingResponseDTO updateTooling(Long id, ToolingRequestDTO dto) {
        Tooling tooling = findOrThrow(id);

        // RETIRED is terminal — a retired tool cannot be brought back to ACTIVE/WORN
        // without a real re-inspection process, which doesn't exist yet. Everything
        // else (name, type, location) can still be edited even while retired, for
        // record-keeping (e.g. fixing a typo in the name).
        if (tooling.getStatus() == ToolingStatus.RETIRED && dto.getStatus() != null
                && !"RETIRED".equals(dto.getStatus())) {
            throw new IllegalStateException(
                    "Tool " + tooling.getToolCode() + " is RETIRED and cannot be reactivated");
        }

        tooling.setName(dto.getName());
        tooling.setToolingType(ToolingType.valueOf(dto.getToolingType()));
        tooling.setLocation(dto.getLocation());
        if (dto.getStatus() != null) {
            tooling.setStatus(ToolingStatus.valueOf(dto.getStatus()));
        }

        Tooling saved = toolingRepository.save(tooling);
        log.info("Tooling updated — toolCode: {}", saved.getToolCode());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ToolingResponseDTO> listTooling(String status, String toolingType) {
        List<Tooling> tools = toolingRepository.findAll();
        if (status != null) {
            ToolingStatus s = ToolingStatus.valueOf(status);
            tools = tools.stream().filter(t -> t.getStatus() == s).toList();
        }
        if (toolingType != null) {
            ToolingType t = ToolingType.valueOf(toolingType);
            tools = tools.stream().filter(x -> x.getToolingType() == t).toList();
        }
        return tools.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ToolingResponseDTO getToolingById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public void deleteTooling(Long id) {
        Tooling tooling = findOrThrow(id);
        if (tooling.getOccupancy() == ToolingOccupancy.IN_OPERATION) {
            throw new IllegalStateException(
                    "Cannot delete " + tooling.getToolCode() + " — it is currently in use on a production stage");
        }
        toolingRepository.delete(tooling);
        log.info("Tooling deleted — toolCode: {}", tooling.getToolCode());
    }

    @Override
    @Transactional
    public ToolingResponseDTO occupyTooling(Long id, Long stageLogId) {
        Tooling tooling = findOrThrow(id);
        if (tooling.getStatus() != ToolingStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Tool " + tooling.getToolCode() + " is not ACTIVE (current: " + tooling.getStatus() + ")");
        }
        if (tooling.getOccupancy() == ToolingOccupancy.IN_OPERATION) {
            throw new IllegalStateException(
                    "Tool " + tooling.getToolCode() + " is currently in use on another operation");
        }

        tooling.setOccupancy(ToolingOccupancy.IN_OPERATION);
        tooling.setCurrentStageLogId(stageLogId);
        Tooling saved = toolingRepository.save(tooling);
        log.info("Tooling occupied — toolCode: {}, stageLogId: {}", saved.getToolCode(), stageLogId);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public ToolingResponseDTO releaseTooling(Long id) {
        Tooling tooling = findOrThrow(id);
        tooling.setOccupancy(ToolingOccupancy.FREE);
        tooling.setCurrentStageLogId(null);
        Tooling saved = toolingRepository.save(tooling);
        log.info("Tooling released — toolCode: {}", saved.getToolCode());
        return toDTO(saved);
    }

    private Tooling findOrThrow(Long id) {
        return toolingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tooling not found with ID: " + id));
    }

    private ToolingResponseDTO toDTO(Tooling t) {
        return ToolingResponseDTO.builder()
                .id(t.getId())
                .toolCode(t.getToolCode())
                .name(t.getName())
                .toolingType(t.getToolingType().name())
                .location(t.getLocation())
                .status(t.getStatus().name())
                .occupancy(t.getOccupancy().name())
                .currentStageLogId(t.getCurrentStageLogId())
                .version(t.getVersion())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
