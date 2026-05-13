package com.aerospace.production_service.service.impl;

import com.aerospace.production_service.dto.DashboardResponseDTO;
import com.aerospace.production_service.entity.ProductionOrderStatus;
import com.aerospace.production_service.repository.ProductionOrderRepository;
import com.aerospace.production_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductionOrderRepository orderRepository;

    @Override
    public DashboardResponseDTO getDashboardStats() {

        long total       = orderRepository.count();
        long planned     = orderRepository.countByStatus(ProductionOrderStatus.PLANNED);
        long released    = orderRepository.countByStatus(ProductionOrderStatus.RELEASED);
        long inProd      = orderRepository.countByStatus(ProductionOrderStatus.IN_PRODUCTION);
        long qaCheck     = orderRepository.countByStatus(ProductionOrderStatus.QUALITY_CHECK);
        long completed   = orderRepository.countByStatus(ProductionOrderStatus.COMPLETED);
        long onHold      = orderRepository.countByStatus(ProductionOrderStatus.ON_HOLD);
        long cancelled   = orderRepository.countByStatus(ProductionOrderStatus.CANCELLED);

        Long totalPlanned  = orderRepository.sumPlannedQuantity();
        Long totalProduced = orderRepository.sumProducedQuantity();
        Long totalRejected = orderRepository.sumRejectedQuantity();

        long safePlanned  = totalPlanned  != null ? totalPlanned  : 0L;
        long safeProduced = totalProduced != null ? totalProduced : 0L;
        long safeRejected = totalRejected != null ? totalRejected : 0L;

        double completionRate = safePlanned > 0
                ? (safeProduced * 100.0) / safePlanned : 0.0;
        double rejectionRate = safeProduced > 0
                ? (safeRejected * 100.0) / safeProduced : 0.0;

        // Group by priority
        Map<String, Long> byPriority = orderRepository.countGroupedByPriority()
                .stream().collect(Collectors.toMap(
                        r -> r[0].toString(),
                        r -> (Long) r[1]));

        // Group by status
        Map<String, Long> byStatus = orderRepository.countGroupedByStatus()
                .stream().collect(Collectors.toMap(
                        r -> r[0].toString(),
                        r -> (Long) r[1]));

        // Group by product name
        Map<String, Long> byProduct = orderRepository.countGroupedByProduct()
                .stream().collect(Collectors.toMap(
                        r -> r[0].toString(),
                        r -> (Long) r[1]));

        return DashboardResponseDTO.builder()
                .totalOrders(total)
                .plannedOrders(planned + released)   // PLANNED + RELEASED together
                .inProductionOrders(inProd)
                .qualityCheckOrders(qaCheck)
                .completedOrders(completed)
                .onHoldOrders(onHold)
                .cancelledOrders(cancelled)
                .totalPlannedQty(safePlanned)
                .totalProducedQty(safeProduced)
                .totalRejectedQty(safeRejected)
                .overallCompletionRate(Math.round(completionRate * 100.0) / 100.0)
                .overallRejectionRate(Math.round(rejectionRate * 100.0) / 100.0)
                .ordersByPriority(byPriority)
                .ordersByStatus(byStatus)
                .ordersByProduct(byProduct)
                .build();
    }
}
