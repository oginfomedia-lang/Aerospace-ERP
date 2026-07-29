package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.entity.Supplier;
import com.aerospace.procurement_service.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suppliers/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class MasterDataReportController {

    private final SupplierRepository supplierRepository;

    @GetMapping("/inactive")
    public ResponseEntity<List<Supplier>> getInactiveSuppliers() {
        List<Supplier> inactive = supplierRepository.findAll().stream()
                .filter(s -> !s.isActive())
                .collect(Collectors.toList());
        return ResponseEntity.ok(inactive);
    }

    @GetMapping("/duplicates")
    public ResponseEntity<List<Supplier>> getDuplicateSuppliers() {
        Map<String, List<Supplier>> byName = supplierRepository.findAll().stream()
                .collect(Collectors.groupingBy(s -> s.getName().trim().toLowerCase()));
        List<Supplier> duplicates = byName.values().stream()
                .filter(group -> group.size() > 1)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return ResponseEntity.ok(duplicates);
    }
}