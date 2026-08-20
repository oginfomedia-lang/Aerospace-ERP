package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.CostCenterDTO;
import com.aerospace.finance_service.service.CostCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/cost-centers")
@RequiredArgsConstructor
public class CostCenterController {

    private final CostCenterService costCenterService;

    @GetMapping
    public ApiResponse<List<CostCenterDTO>> getAll() {
        return new ApiResponse<>("Cost centers fetched", costCenterService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<CostCenterDTO> getById(@PathVariable Long id) {
        return new ApiResponse<>("Cost center fetched", costCenterService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "CostCenter")
    public ResponseEntity<ApiResponse<CostCenterDTO>> create(@Valid @RequestBody CostCenterDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Cost center created", costCenterService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "UPDATE", entityType = "CostCenter")
    public ApiResponse<CostCenterDTO> update(@PathVariable Long id, @Valid @RequestBody CostCenterDTO dto) {
        return new ApiResponse<>("Cost center updated", costCenterService.update(id, dto));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "DEACTIVATE", entityType = "CostCenter")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        costCenterService.deactivate(id);
        return new ApiResponse<>("Cost center deactivated", null);
    }
}
