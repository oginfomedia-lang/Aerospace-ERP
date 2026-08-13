package com.aerospace.sales_service.service.impl;

import com.aerospace.sales_service.dto.SalesReportSummaryDTO;
import com.aerospace.sales_service.entity.*;
import com.aerospace.sales_service.repository.DispatchRepository;
import com.aerospace.sales_service.repository.SalesOrderRepository;
import com.aerospace.sales_service.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesReportServiceImpl implements SalesReportService {

    private final SalesOrderRepository salesOrderRepository;
    private final DispatchRepository dispatchRepository;

    @Override
    @Transactional(readOnly = true)
    public SalesReportSummaryDTO getSummary(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        long orderIntake = salesOrderRepository.countByCreatedAtAfter(since);

        List<SalesOrder> openOrders = salesOrderRepository.findByStatusInAndIsCurrentRevisionTrue(
                List.of(SalesOrderStatus.CONFIRMED, SalesOrderStatus.ALLOCATED));
        long backordered = openOrders.stream()
                .filter(o -> o.getLines().stream().anyMatch(l -> l.getQuantityAllocated() < l.getQuantity()))
                .count();

        List<Dispatch> dispatched = dispatchRepository.findByStatus(DispatchStatus.DISPATCHED).stream()
                .filter(d -> d.getDispatchedAt() != null && d.getDispatchedAt().isAfter(since))
                .toList();
        long onTime = dispatched.stream()
                .filter(d -> d.getSalesOrder().getPromisedDeliveryDate() == null
                        || !d.getDispatchedAt().toLocalDate().isAfter(d.getSalesOrder().getPromisedDeliveryDate()))
                .count();
        double onTimePct = dispatched.isEmpty() ? 0 : (onTime * 100.0 / dispatched.size());

        return SalesReportSummaryDTO.builder()
                .periodDays(days)
                .orderIntakeCount(orderIntake)
                .openOrdersCount(openOrders.size())
                .backorderedOrdersCount(backordered)
                .onTimeDeliveryPercent(Math.round(onTimePct * 100) / 100.0)
                .dispatchedCount(dispatched.size())
                .build();
    }
}