package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.ProfitCenterDTO;
import com.aerospace.finance_service.service.ProfitCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/profit-centers")
@RequiredArgsConstructor
public class ProfitCenterController {

    private final ProfitCenterService profitCenterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    public ApiResponse<List<ProfitCenterDTO>> getAll() {
        return new ApiResponse<>("Profit centers fetched", profitCenterService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    public ApiResponse<ProfitCenterDTO> getById(@PathVariable Long id) {
        return new ApiResponse<>("Profit center fetched", profitCenterService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "CREATE", entityType = "ProfitCenter")
    public ResponseEntity<ApiResponse<ProfitCenterDTO>> create(@Valid @RequestBody ProfitCenterDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Profit center created", profitCenterService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "UPDATE", entityType = "ProfitCenter")
    public ApiResponse<ProfitCenterDTO> update(@PathVariable Long id, @Valid @RequestBody ProfitCenterDTO dto) {
        return new ApiResponse<>("Profit center updated", profitCenterService.update(id, dto));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','MANAGER','ADMIN')")
    @Audited(action = "DEACTIVATE", entityType = "ProfitCenter")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        profitCenterService.deactivate(id);
        return new ApiResponse<>("Profit center deactivated", null);
    }
}
