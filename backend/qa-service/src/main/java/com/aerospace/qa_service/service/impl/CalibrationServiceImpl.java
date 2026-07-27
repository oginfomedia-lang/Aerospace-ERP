package com.aerospace.qa_service.service.impl;

import com.aerospace.qa_service.dto.CalibrationRecordResponseDTO;
import com.aerospace.qa_service.dto.CalibrationRequestDTO;
import com.aerospace.qa_service.dto.EquipmentRequestDTO;
import com.aerospace.qa_service.dto.EquipmentResponseDTO;
import com.aerospace.qa_service.entity.CalibrationRecord;
import com.aerospace.qa_service.entity.CalibrationResult;
import com.aerospace.qa_service.entity.Equipment;
import com.aerospace.qa_service.entity.EquipmentStatus;
import com.aerospace.qa_service.entity.EquipmentType;
import com.aerospace.qa_service.exception.ResourceNotFoundException;
import com.aerospace.qa_service.repository.CalibrationRecordRepository;
import com.aerospace.qa_service.repository.EquipmentRepository;
import com.aerospace.qa_service.service.CalibrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalibrationServiceImpl implements CalibrationService {

    private final EquipmentRepository equipmentRepository;
    private final CalibrationRecordRepository calibrationRecordRepository;

    @Override
    @Transactional
    public EquipmentResponseDTO registerEquipment(EquipmentRequestDTO dto) {
        if (equipmentRepository.existsByAssetTag(dto.getAssetTag())) {
            throw new IllegalStateException("An equipment item with asset tag '" + dto.getAssetTag() + "' already exists");
        }

        Equipment equipment = Equipment.builder()
                .assetTag(dto.getAssetTag())
                .name(dto.getName())
                .equipmentType(EquipmentType.valueOf(dto.getEquipmentType()))
                .manufacturer(dto.getManufacturer())
                .modelNumber(dto.getModelNumber())
                .location(dto.getLocation())
                .calibrationFrequencyDays(dto.getCalibrationFrequencyDays())
                .status(EquipmentStatus.ACTIVE)
                .build();

        Equipment saved = equipmentRepository.save(equipment);
        log.info("Equipment registered — assetTag: {}, name: {}", saved.getAssetTag(), saved.getName());
        return toEquipmentDTO(saved);
    }

    @Override
    public List<EquipmentResponseDTO> listEquipment(String status) {
        List<Equipment> equipment = status == null
                ? equipmentRepository.findAll()
                : equipmentRepository.findByStatus(EquipmentStatus.valueOf(status));
        return equipment.stream().map(this::toEquipmentDTO).toList();
    }

    @Override
    public EquipmentResponseDTO getEquipmentById(Long id) {
        return toEquipmentDTO(findEquipmentOrThrow(id));
    }

    @Override
    @Transactional
    public CalibrationRecordResponseDTO recordCalibration(Long equipmentId, CalibrationRequestDTO dto) {
        Equipment equipment = findEquipmentOrThrow(equipmentId);

        LocalDateTime calibratedAt = dto.getCalibratedAt() != null ? dto.getCalibratedAt() : LocalDateTime.now();
        LocalDateTime nextDueAt = calibratedAt.plusDays(equipment.getCalibrationFrequencyDays());

        CalibrationRecord record = CalibrationRecord.builder()
                .equipment(equipment)
                .calibratedAt(calibratedAt)
                .performedBy(dto.getPerformedBy())
                .certificateNumber(dto.getCertificateNumber())
                .result(CalibrationResult.valueOf(dto.getResult()))
                .remarks(dto.getRemarks())
                .nextDueAt(nextDueAt)
                .build();

        CalibrationRecord saved = calibrationRecordRepository.save(record);

        equipment.setLastCalibratedAt(calibratedAt);
        equipment.setNextCalibrationDueAt(nextDueAt);
        equipmentRepository.save(equipment);

        log.info("Calibration recorded — assetTag: {}, result: {}, nextDueAt: {}",
                equipment.getAssetTag(), saved.getResult(), nextDueAt);
        return toRecordDTO(saved);
    }

    @Override
    public List<CalibrationRecordResponseDTO> getHistory(Long equipmentId) {
        return calibrationRecordRepository.findByEquipmentIdOrderByCalibratedAtDesc(equipmentId)
                .stream().map(this::toRecordDTO).toList();
    }

    @Override
    public List<EquipmentResponseDTO> getDueList(int withinDays) {
        LocalDateTime cutoff = LocalDateTime.now().plusDays(withinDays);
        return equipmentRepository.findByNextCalibrationDueAtBefore(cutoff)
                .stream().map(this::toEquipmentDTO).toList();
    }

    private Equipment findEquipmentOrThrow(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + id));
    }

    private EquipmentResponseDTO toEquipmentDTO(Equipment e) {
        boolean overdue = e.getNextCalibrationDueAt() != null && e.getNextCalibrationDueAt().isBefore(LocalDateTime.now());
        return EquipmentResponseDTO.builder()
                .id(e.getId())
                .assetTag(e.getAssetTag())
                .name(e.getName())
                .equipmentType(e.getEquipmentType().name())
                .manufacturer(e.getManufacturer())
                .modelNumber(e.getModelNumber())
                .location(e.getLocation())
                .calibrationFrequencyDays(e.getCalibrationFrequencyDays())
                .status(e.getStatus().name())
                .lastCalibratedAt(e.getLastCalibratedAt())
                .nextCalibrationDueAt(e.getNextCalibrationDueAt())
                .overdue(overdue)
                .createdAt(e.getCreatedAt())
                .build();
    }

    private CalibrationRecordResponseDTO toRecordDTO(CalibrationRecord r) {
        return CalibrationRecordResponseDTO.builder()
                .id(r.getId())
                .equipmentId(r.getEquipment().getId())
                .assetTag(r.getEquipment().getAssetTag())
                .calibratedAt(r.getCalibratedAt())
                .performedBy(r.getPerformedBy())
                .certificateNumber(r.getCertificateNumber())
                .result(r.getResult().name())
                .remarks(r.getRemarks())
                .nextDueAt(r.getNextDueAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
