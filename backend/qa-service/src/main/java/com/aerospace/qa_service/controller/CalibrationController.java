package com.aerospace.qa_service.controller;

import com.aerospace.qa_service.common.ApiResponse;
import com.aerospace.qa_service.dto.CalibrationRecordResponseDTO;
import com.aerospace.qa_service.dto.CalibrationRequestDTO;
import com.aerospace.qa_service.dto.EquipmentRequestDTO;
import com.aerospace.qa_service.dto.EquipmentResponseDTO;
import com.aerospace.qa_service.service.CalibrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/qa/calibration")
@RequiredArgsConstructor
public class CalibrationController {

    private final CalibrationService calibrationService;

    // POST /api/v1/qa/calibration/equipment — register a new gauge/tool/instrument
    @PostMapping("/equipment")
    public ResponseEntity<ApiResponse<EquipmentResponseDTO>> registerEquipment(
            @Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("EQUIPMENT_REGISTERED", calibrationService.registerEquipment(dto)));
    }

    // GET /api/v1/qa/calibration/equipment?status=ACTIVE
    @GetMapping("/equipment")
    public ResponseEntity<ApiResponse<List<EquipmentResponseDTO>>> listEquipment(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", calibrationService.listEquipment(status)));
    }

    // GET /api/v1/qa/calibration/equipment/due?withinDays=30 — due-soon + overdue list
    @GetMapping("/equipment/due")
    public ResponseEntity<ApiResponse<List<EquipmentResponseDTO>>> getDueList(
            @RequestParam(defaultValue = "30") int withinDays) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", calibrationService.getDueList(withinDays)));
    }

    // GET /api/v1/qa/calibration/equipment/5
    @GetMapping("/equipment/{id}")
    public ResponseEntity<ApiResponse<EquipmentResponseDTO>> getEquipment(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", calibrationService.getEquipmentById(id)));
    }

    // POST /api/v1/qa/calibration/equipment/5/calibrate — record a calibration event
    @PostMapping("/equipment/{id}/calibrate")
    public ResponseEntity<ApiResponse<CalibrationRecordResponseDTO>> recordCalibration(
            @PathVariable Long id, @Valid @RequestBody CalibrationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("CALIBRATION_RECORDED", calibrationService.recordCalibration(id, dto)));
    }

    // GET /api/v1/qa/calibration/equipment/5/history
    @GetMapping("/equipment/{id}/history")
    public ResponseEntity<ApiResponse<List<CalibrationRecordResponseDTO>>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", calibrationService.getHistory(id)));
    }
}
