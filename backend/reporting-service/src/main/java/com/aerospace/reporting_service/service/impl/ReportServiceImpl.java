package com.aerospace.reporting_service.service.impl;

import com.aerospace.reporting_service.dto.*;
import com.aerospace.reporting_service.entity.InventoryFact;
import com.aerospace.reporting_service.entity.ProductionFact;
import com.aerospace.reporting_service.entity.WorkOrderFact;
import com.aerospace.reporting_service.repository.*;
import com.aerospace.reporting_service.service.ReportService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final InventoryFactRepository inventoryFactRepository;
    private final WorkOrderFactRepository workOrderFactRepository;
    private final ProductionFactRepository productionFactRepository;
    private final QAFactRepository qaFactRepository;
    private final ProcurementFactRepository procurementFactRepository;

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        long totalParts = inventoryFactRepository.count();
        long pendingQA = inventoryFactRepository.countByStatus("PENDING_QA");
        long rejectedItems = inventoryFactRepository.countByStatus("REJECTED");
        long lowStockCount = inventoryFactRepository.countByStatus("LOW_STOCK"); // you'll need to track this via StockLowEvent

        double inventoryValue = inventoryFactRepository.findAll().stream()
                .mapToDouble(f -> f.getQuantity() * (f.getUnitPrice() != null ? f.getUnitPrice() : 0))
                .sum();

        long totalWorkOrders = workOrderFactRepository.count();
        long completedWorkOrders = workOrderFactRepository.countByStatus("COMPLETED");
        double completionRate = totalWorkOrders == 0 ? 0 : (completedWorkOrders * 100.0 / totalWorkOrders);

        long openProductionOrders = productionFactRepository.findAll().stream()
                .filter(p -> !"COMPLETED".equals(p.getStatus()))
                .count();

        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setInventoryValue(inventoryValue);
        dto.setTotalWorkOrders(totalWorkOrders);
        dto.setCompletionRate(completionRate);
        dto.setOpenProductionOrders(openProductionOrders);
        dto.setPendingQAItems(pendingQA);
        dto.setLowStockCount(lowStockCount);
        return dto;
    }

    @Override
    public List<InventoryValueDTO> getInventoryValueByPart() {
        return inventoryFactRepository.findAll().stream()
                .collect(Collectors.groupingBy(InventoryFact::getPartName,
                        Collectors.summingDouble(f -> f.getQuantity() * (f.getUnitPrice() != null ? f.getUnitPrice() : 0))))
                .entrySet().stream()
                .map(e -> new InventoryValueDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<QATrendDTO> getQATrends() {
        long approved = qaFactRepository.countByDecision("APPROVED");
        long rejected = qaFactRepository.countByDecision("REJECTED");
        return List.of(new QATrendDTO("APPROVED", approved), new QATrendDTO("REJECTED", rejected));
    }

    @Override
    public List<ProductionYieldDTO> getProductionYield() {
        return productionFactRepository.findAll().stream()
                .map(p -> new ProductionYieldDTO(p.getPoNumber(), p.getPlannedQuantity(),
                        p.getProducedQuantity() != null ? p.getProducedQuantity() : 0,
                        p.getRejectedQuantity() != null ? p.getRejectedQuantity() : 0))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkOrderCycleDTO> getWorkOrderCycleTimes() {
        return workOrderFactRepository.findAll().stream()
                .filter(wo -> wo.getDaysToComplete() != null)
                .map(wo -> new WorkOrderCycleDTO(wo.getWoNumber(), wo.getDaysToComplete()))
                .collect(Collectors.toList());
    }
}