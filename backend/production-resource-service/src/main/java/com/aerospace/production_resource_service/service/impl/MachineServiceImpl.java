package com.aerospace.production_resource_service.service.impl;

import com.aerospace.production_resource_service.client.QaServiceClient;
import com.aerospace.production_resource_service.client.dto.EquipmentInfo;
import com.aerospace.production_resource_service.dto.*;
import com.aerospace.production_resource_service.entity.*;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.MachineMaintenanceRecordRepository;
import com.aerospace.production_resource_service.repository.MachineRepository;
import com.aerospace.production_resource_service.service.MachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MachineServiceImpl implements MachineService {

    private final MachineRepository machineRepository;
    private final MachineMaintenanceRecordRepository maintenanceRecordRepository;
    private final QaServiceClient qaServiceClient;

    @Override
    @Transactional
    public MachineResponseDTO registerMachine(MachineRequestDTO dto) {
        if (machineRepository.existsByMachineCode(dto.getMachineCode())) {
            throw new IllegalStateException("A machine with code '" + dto.getMachineCode() + "' already exists");
        }

        Machine machine = Machine.builder()
                .machineCode(dto.getMachineCode())
                .name(dto.getName())
                .machineType(MachineType.valueOf(dto.getMachineType()))
                .manufacturer(dto.getManufacturer())
                .modelNumber(dto.getModelNumber())
                .location(dto.getLocation())
                .capacity(dto.getCapacity())
                .maintenanceFrequencyDays(dto.getMaintenanceFrequencyDays())
                .linkedEquipmentId(dto.getLinkedEquipmentId())
                .status(MachineStatus.ACTIVE)
                .build();

        Machine saved = machineRepository.save(machine);
        log.info("Machine registered — machineCode: {}, name: {}", saved.getMachineCode(), saved.getName());
        return toMachineDTO(saved);
    }

    @Override
    public List<MachineResponseDTO> listMachines(String status) {
        List<Machine> machines = status == null
                ? machineRepository.findAll()
                : machineRepository.findByStatus(MachineStatus.valueOf(status));
        return machines.stream().map(this::toMachineDTO).toList();
    }

    @Override
    public MachineResponseDTO getMachineById(Long id) {
        return toMachineDTO(findMachineOrThrow(id));
    }

    @Override
    @Transactional
    public MaintenanceRecordResponseDTO recordMaintenance(Long machineId, MaintenanceRequestDTO dto) {
        Machine machine = findMachineOrThrow(machineId);

        LocalDateTime performedAt = dto.getPerformedAt() != null ? dto.getPerformedAt() : LocalDateTime.now();
        LocalDateTime nextDueAt = performedAt.plusDays(machine.getMaintenanceFrequencyDays());

        MachineMaintenanceRecord record = MachineMaintenanceRecord.builder()
                .machine(machine)
                .maintenanceType(MaintenanceType.valueOf(dto.getMaintenanceType()))
                .performedAt(performedAt)
                .performedBy(dto.getPerformedBy())
                .cost(dto.getCost())
                .downtimeMinutes(dto.getDowntimeMinutes())
                .result(MaintenanceResult.valueOf(dto.getResult()))
                .remarks(dto.getRemarks())
                .nextDueAt(nextDueAt)
                .build();

        MachineMaintenanceRecord saved = maintenanceRecordRepository.save(record);

        machine.setLastMaintenanceAt(performedAt);
        machine.setNextMaintenanceDueAt(nextDueAt);
        machineRepository.save(machine);

        log.info("Maintenance recorded — machineCode: {}, result: {}, nextDueAt: {}",
                machine.getMachineCode(), saved.getResult(), nextDueAt);
        return toRecordDTO(saved);
    }

    @Override
    public List<MaintenanceRecordResponseDTO> getMaintenanceHistory(Long machineId) {
        return maintenanceRecordRepository.findByMachineIdOrderByPerformedAtDesc(machineId)
                .stream().map(this::toRecordDTO).toList();
    }

    @Override
    public List<MachineResponseDTO> getDueList(int withinDays) {
        LocalDateTime cutoff = LocalDateTime.now().plusDays(withinDays);
        return machineRepository.findByNextMaintenanceDueAtBefore(cutoff)
                .stream().map(this::toMachineDTO).toList();
    }

    @Override
    @Transactional
    public MachineResponseDTO occupyMachine(Long id, Long stageLogId) {
        Machine machine = findMachineOrThrow(id);
        if (machine.getStatus() != MachineStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Machine " + machine.getMachineCode() + " is not ACTIVE (current: " + machine.getStatus() + ")");
        }
        boolean overdue = machine.getNextMaintenanceDueAt() != null
                && machine.getNextMaintenanceDueAt().isBefore(LocalDateTime.now());
        if (overdue) {
            throw new IllegalStateException(
                    "Machine " + machine.getMachineCode() + " is overdue for maintenance and cannot be assigned");
        }
        if (machine.getOccupancy() == MachineOccupancy.IN_OPERATION) {
            throw new IllegalStateException(
                    "Machine " + machine.getMachineCode() + " is currently in use on another operation");
        }
        if (machine.getLinkedEquipmentId() != null) {
            // Best-effort by design: if qa-service is unreachable, don't block every
            // machine assignment in the plant on a second service being up — log and
            // proceed. If it IS reachable and says overdue, that's a hard block.
            try {
                EquipmentInfo equipment = qaServiceClient.getEquipment(machine.getLinkedEquipmentId());
                if (equipment != null && equipment.isOverdue()) {
                    throw new IllegalStateException(
                            "Machine " + machine.getMachineCode() + "'s linked calibration ("
                                    + equipment.getAssetTag() + ") is overdue and must be recalibrated before use");
                }
            } catch (IllegalStateException ex) {
                throw ex; // the overdue rejection above — let it propagate
            } catch (org.springframework.web.client.RestClientException ex) {
                log.warn("[QA_SERVICE] Could not verify calibration for machine {} (linkedEquipmentId={}): {}",
                        machine.getMachineCode(), machine.getLinkedEquipmentId(), ex.getMessage());
            }
        }

        machine.setOccupancy(MachineOccupancy.IN_OPERATION);
        machine.setCurrentStageLogId(stageLogId);
        Machine saved = machineRepository.save(machine);
        log.info("Machine occupied — machineCode: {}, stageLogId: {}", saved.getMachineCode(), stageLogId);
        return toMachineDTO(saved);
    }

    @Override
    @Transactional
    public MachineResponseDTO releaseMachine(Long id) {
        Machine machine = findMachineOrThrow(id);
        machine.setOccupancy(MachineOccupancy.FREE);
        machine.setCurrentStageLogId(null);
        Machine saved = machineRepository.save(machine);
        log.info("Machine released — machineCode: {}", saved.getMachineCode());
        return toMachineDTO(saved);
    }

    private Machine findMachineOrThrow(Long id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with ID: " + id));
    }

    private MachineResponseDTO toMachineDTO(Machine m) {
        boolean overdue = m.getNextMaintenanceDueAt() != null && m.getNextMaintenanceDueAt().isBefore(LocalDateTime.now());
        return MachineResponseDTO.builder()
                .id(m.getId())
                .machineCode(m.getMachineCode())
                .name(m.getName())
                .machineType(m.getMachineType().name())
                .manufacturer(m.getManufacturer())
                .modelNumber(m.getModelNumber())
                .location(m.getLocation())
                .capacity(m.getCapacity())
                .maintenanceFrequencyDays(m.getMaintenanceFrequencyDays())
                .status(m.getStatus().name())
                .linkedEquipmentId(m.getLinkedEquipmentId())
                .lastMaintenanceAt(m.getLastMaintenanceAt())
                .nextMaintenanceDueAt(m.getNextMaintenanceDueAt())
                .overdue(overdue)
                .occupancy(m.getOccupancy().name())
                .currentStageLogId(m.getCurrentStageLogId())
                .version(m.getVersion())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private MaintenanceRecordResponseDTO toRecordDTO(MachineMaintenanceRecord r) {
        return MaintenanceRecordResponseDTO.builder()
                .id(r.getId())
                .machineId(r.getMachine().getId())
                .machineCode(r.getMachine().getMachineCode())
                .maintenanceType(r.getMaintenanceType().name())
                .performedAt(r.getPerformedAt())
                .performedBy(r.getPerformedBy())
                .cost(r.getCost())
                .downtimeMinutes(r.getDowntimeMinutes())
                .result(r.getResult().name())
                .remarks(r.getRemarks())
                .nextDueAt(r.getNextDueAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}