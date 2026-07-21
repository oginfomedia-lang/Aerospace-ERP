package com.aerospace.inventory_service.service.impl;

import com.aerospace.inventory_service.dto.DashboardResponseDTO;
import com.aerospace.inventory_service.repository.PartRepository;
import com.aerospace.inventory_service.repository.StockBalanceRepository;
import com.aerospace.inventory_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable; // FIX 1: was jakarta.persistence.Cacheable
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService { // FIX 2: was missing implements

    private final PartRepository partRepository;
    private final StockBalanceRepository balanceRepository;

    @Override
    @Cacheable("dashboardStats")
    public DashboardResponseDTO getDashboardStats() {

        // fetch once into variable — was calling getTotalApprovedStock() twice (two DB hits)
        Long approvedRaw = balanceRepository.getTotalApprovedStock();
        Long pendingQARaw = balanceRepository.getTotalPendingQA();
        Long rejectedRaw = balanceRepository.countRejectedLots();

        long totalApprovedStock = approvedRaw  != null ? approvedRaw  : 0L;
        long pendingQA          = pendingQARaw != null ? pendingQARaw : 0L;
        long rejected           = rejectedRaw  != null ? rejectedRaw  : 0L;

        // Convert List<Object[]> -> Map<partType, count> for frontend chart
        // row[0] (partType) can be null for parts with no type set — Jackson can't
        // serialize a null map key, so fall back to a label instead of dropping/crashing.
        Map<String, Long> typeMap = partRepository.countPartsByType().stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? (String) row[0] : "UNSPECIFIED",
                        row -> (Long) row[1]
                ));

        // Low stock = approved quantity <= reorderLevel, same rule as the frontend's
        // LowStockAlert — only parts that have set a reorder level are considered.
        Map<Long, Long> approvedByPart = balanceRepository.getApprovedQuantityByPart().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        long lowStock = partRepository.findIdAndReorderLevelWhereReorderLevelSet().stream()
                .filter(row -> {
                    long approvedQty = approvedByPart.getOrDefault((Long) row[0], 0L);
                    return approvedQty <= (Integer) row[1];
                })
                .count();

        return DashboardResponseDTO.builder()
                .totalParts(partRepository.count())
                .totalQuantity(totalApprovedStock)
                .pendingQAItems(pendingQA)
                .rejectedItems(rejected)
                .lowStockItems(lowStock)
                .partsByType(typeMap)
                .inventoryValue(0.0) // Wire to Finance service pricing later
                .build();
    }
}