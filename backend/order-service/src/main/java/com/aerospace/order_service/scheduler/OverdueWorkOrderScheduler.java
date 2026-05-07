package com.aerospace.order_service.scheduler;

import com.aerospace.order_service.entity.WorkOrder;
import com.aerospace.order_service.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueWorkOrderScheduler {

    private final WorkOrderRepository workOrderRepository;

    /**
     * Runs every hour — logs overdue WOs.
     * Extend this to publish Kafka events or send alerts when overdue count changes.
     */
    @Scheduled(fixedRate = 3600000)
    public void checkOverdueWorkOrders() {
        List<WorkOrder> overdue = workOrderRepository.findOverdue(LocalDateTime.now());
        if (overdue.isEmpty()) {
            log.debug("[SCHEDULER] No overdue work orders.");
            return;
        }
        log.warn("[SCHEDULER] {} overdue work order(s) found:", overdue.size());
        overdue.forEach(wo ->
            log.warn("  → {} | {} | Priority={} | DueDate={} | AssignedTo={}",
                    wo.getWoNumber(), wo.getTaskDescription(),
                    wo.getPriority(), wo.getDueDate(), wo.getAssignedTo())
        );
    }
}