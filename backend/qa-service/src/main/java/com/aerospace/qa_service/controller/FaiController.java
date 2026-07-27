package com.aerospace.qa_service.controller;

import com.aerospace.qa_service.common.ApiResponse;
import com.aerospace.qa_service.dto.FaiCreateRequestDTO;
import com.aerospace.qa_service.dto.FaiPerformRequestDTO;
import com.aerospace.qa_service.dto.FaiResponseDTO;
import com.aerospace.qa_service.service.FaiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/qa/fai")
@RequiredArgsConstructor
public class FaiController {

    private final FaiService faiService;

    @PostMapping
    public ResponseEntity<ApiResponse<FaiResponseDTO>> createManual(@Valid @RequestBody FaiCreateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("FAI_CREATED", faiService.createManual(dto)));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<FaiResponseDTO>>> getPending() {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", faiService.getPendingQueue()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FaiResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", faiService.getById(id)));
    }

    @GetMapping("/part/{partId}/history")
    public ResponseEntity<ApiResponse<List<FaiResponseDTO>>> getByPart(@PathVariable Long partId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", faiService.getByPartId(partId)));
    }

    @PostMapping("/{id}/perform")
    public ResponseEntity<ApiResponse<FaiResponseDTO>> perform(
            @PathVariable Long id, @Valid @RequestBody FaiPerformRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("FAI_COMPLETED", faiService.performFai(id, dto)));
    }
}
