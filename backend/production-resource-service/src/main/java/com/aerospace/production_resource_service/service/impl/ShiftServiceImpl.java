package com.aerospace.production_resource_service.service.impl;

import com.aerospace.production_resource_service.dto.ShiftRequestDTO;
import com.aerospace.production_resource_service.dto.ShiftResponseDTO;
import com.aerospace.production_resource_service.entity.Shift;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.ShiftRepository;
import com.aerospace.production_resource_service.service.ShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;

    @Override
    @Transactional
    public ShiftResponseDTO createShift(ShiftRequestDTO dto) {
        if (shiftRepository.existsByName(dto.getName())) {
            throw new IllegalStateException("A shift named '" + dto.getName() + "' already exists");
        }
        Shift shift = Shift.builder()
                .name(dto.getName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .daysOfWeek(dto.getDaysOfWeek())
                .build();
        Shift saved = shiftRepository.save(shift);
        log.info("Shift created — name: {}", saved.getName());
        return toDTO(saved);
    }

    @Override
    public List<ShiftResponseDTO> listShifts() {
        return shiftRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public ShiftResponseDTO getShiftById(Long id) {
        return toDTO(findOrThrow(id));
    }

    private Shift findOrThrow(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with ID: " + id));
    }

    private ShiftResponseDTO toDTO(Shift s) {
        return ShiftResponseDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .daysOfWeek(s.getDaysOfWeek())
                .build();
    }
}
