package com.aerospace.qa_service.service;

import com.aerospace.qa_service.dto.CalibrationRecordResponseDTO;
import com.aerospace.qa_service.dto.CalibrationRequestDTO;
import com.aerospace.qa_service.dto.EquipmentRequestDTO;
import com.aerospace.qa_service.dto.EquipmentResponseDTO;

import java.util.List;

public interface CalibrationService {

    EquipmentResponseDTO registerEquipment(EquipmentRequestDTO dto);

    List<EquipmentResponseDTO> listEquipment(String status);

    EquipmentResponseDTO getEquipmentById(Long id);

    CalibrationRecordResponseDTO recordCalibration(Long equipmentId, CalibrationRequestDTO dto);

    List<CalibrationRecordResponseDTO> getHistory(Long equipmentId);

    // Due-soon + overdue list — used by the UI banner and the daily scheduler
    List<EquipmentResponseDTO> getDueList(int withinDays);
}
