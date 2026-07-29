package com.aerospace.inventory_service.controller;

import com.aerospace.inventory_service.common.ApiResponse;
import com.aerospace.inventory_service.entity.Part;
import com.aerospace.inventory_service.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Inactive Records + Duplicate Records reports for the Part/Item master.
@RestController
@RequestMapping("/api/v1/inventory/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class MasterDataReportController {

    private final PartRepository partRepository;

    @GetMapping("/inactive-parts")
    public ResponseEntity<ApiResponse<List<Part>>> getInactiveParts() {
        List<Part> inactive = partRepository.findAll().stream()
                .filter(p -> !"ACTIVE".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", inactive));
    }

    @GetMapping("/duplicate-parts")
    public ResponseEntity<ApiResponse<List<Part>>> getDuplicateParts() {
        Map<String, List<Part>> byName = partRepository.findAll().stream()
                .collect(Collectors.groupingBy(p -> p.getName().trim().toLowerCase()));
        List<Part> duplicates = byName.values().stream()
                .filter(group -> group.size() > 1)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", duplicates));
    }
}