package com.aerospace.production_service.service.impl;

import com.aerospace.production_service.dto.RoutingOperationDTO;
import com.aerospace.production_service.dto.RoutingTemplateRequestDTO;
import com.aerospace.production_service.dto.RoutingTemplateResponseDTO;
import com.aerospace.production_service.entity.OperationType;
import com.aerospace.production_service.entity.RoutingOperation;
import com.aerospace.production_service.entity.RoutingTemplate;
import com.aerospace.production_service.exception.ResourceNotFoundException;
import com.aerospace.production_service.mapper.RoutingTemplateMapper;
import com.aerospace.production_service.repository.RoutingTemplateRepository;
import com.aerospace.production_service.service.RoutingTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingTemplateServiceImpl implements RoutingTemplateService {

    private final RoutingTemplateRepository repository;
    private final RoutingTemplateMapper mapper;

    @Override
    @Transactional
    public RoutingTemplateResponseDTO create(RoutingTemplateRequestDTO dto) {
        assertEndsWithQaInspection(dto.getOperations());
        RoutingTemplate template = RoutingTemplate.builder()
                .partNumber(dto.getPartNumber())
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        template.setOperations(toOperationEntities(dto.getOperations(), template));
        RoutingTemplate saved = repository.save(template);
        log.info("[ROUTING] Template created — id={}, partNumber={}, operations={}",
                saved.getId(), saved.getPartNumber(), saved.getOperations().size());
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public RoutingTemplateResponseDTO update(Long id, RoutingTemplateRequestDTO dto) {
        assertEndsWithQaInspection(dto.getOperations());
        RoutingTemplate previous = findOrThrow(id);

        // Never mutate a template's operations in place — ProductionStageLog.routingOperationId
        // is a live, non-FK reference re-resolved at stage-start time (startStage's required-
        // skill/tooling checks); clearing+recreating operations on every edit (the old
        // orphanRemoval-driven behavior) would silently orphan those references for any
        // in-flight order. An edit creates the next revision instead — old rows, and every
        // id inside them, live forever.
        previous.setIsCurrentRevision(false);
        repository.save(previous);

        RoutingTemplate next = RoutingTemplate.builder()
                .partNumber(previous.getPartNumber())
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : previous.getIsActive())
                .revisionNumber(previous.getRevisionNumber() + 1)
                .supersedesId(previous.getId())
                .isCurrentRevision(true)
                .build();
        next.setOperations(toOperationEntities(dto.getOperations(), next));
        RoutingTemplate saved = repository.save(next);
        log.info("[ROUTING] Template revised — partNumber={}, revision={} (supersedes id {})",
                saved.getPartNumber(), saved.getRevisionNumber(), previous.getId());
        return mapper.toDTO(saved);
    }

    // A routing must end in a QA Inspection step — otherwise a BOM line could reach
    // "readyForAssembly" without ever having been quality-checked after its last
    // production operation.
    private void assertEndsWithQaInspection(List<RoutingOperationDTO> operations) {
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException("A routing template needs at least one operation.");
        }
        RoutingOperationDTO last = operations.stream()
                .max(java.util.Comparator.comparing(RoutingOperationDTO::getSequenceNumber))
                .orElseThrow();
        OperationType lastType = last.getOperationType() != null ? last.getOperationType() : OperationType.PRODUCTION;
        if (lastType != OperationType.QA_INSPECTION) {
            throw new IllegalArgumentException(
                    "A routing template must end with a QA Inspection step (last operation was '"
                            + last.getOperationName() + "', type " + lastType + ").");
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RoutingTemplate template = findOrThrow(id);
        repository.delete(template);
    }

    @Override
    @Transactional(readOnly = true)
    public RoutingTemplateResponseDTO getById(Long id) {
        return mapper.toDTO(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutingTemplateResponseDTO> getAll() {
        // Only the current revision per part number — superseded revisions are reachable
        // via getHistory(partNumber), not the main list, so the list stays one row per part.
        return repository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsCurrentRevision()))
                .map(mapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutingTemplateResponseDTO> getByPartNumber(String partNumber) {
        return repository.findByPartNumber(partNumber).stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsCurrentRevision()))
                .map(mapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutingTemplateResponseDTO> getHistory(String partNumber) {
        return repository.findByPartNumberOrderByRevisionNumberDesc(partNumber).stream()
                .map(mapper::toDTO).toList();
    }

    private List<RoutingOperation> toOperationEntities(List<RoutingOperationDTO> dtos, RoutingTemplate template) {
        List<RoutingOperation> ops = new ArrayList<>();
        if (dtos == null) return ops;
        for (RoutingOperationDTO d : dtos) {
            ops.add(RoutingOperation.builder()
                    .routingTemplate(template)
                    .sequenceNumber(d.getSequenceNumber())
                    .operationName(d.getOperationName())
                    .operationType(d.getOperationType() != null ? d.getOperationType() : OperationType.PRODUCTION)
                    .operationCode(d.getOperationCode())
                    .expectedDurationMinutes(d.getExpectedDurationMinutes())
                    .workstation(d.getWorkstation())
                    .instructions(d.getInstructions())
                    .preferredMachineId(d.getPreferredMachineId())
                    .preferredMachineCode(d.getPreferredMachineCode())
                    .preferredEmployeeId(d.getPreferredEmployeeId())
                    .preferredEmployeeName(d.getPreferredEmployeeName())
                    .requiredSkillId(d.getRequiredSkillId())
                    .requiredSkillName(d.getRequiredSkillName())
                    .minimumSkillLevel(d.getMinimumSkillLevel())
                    .requiredToolingId(d.getRequiredToolingId())
                    .requiredToolingName(d.getRequiredToolingName())
                    .build());
        }
        return ops;
    }

    private RoutingTemplate findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Routing template not found: " + id));
    }
}
