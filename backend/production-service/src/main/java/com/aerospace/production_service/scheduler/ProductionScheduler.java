package com.aerospace.production_service.scheduler;

import com.aerospace.production_service.entity.ProductionOrder;
import com.aerospace.production_service.entity.ProductionOrderStatus;
import com.aerospace.production_service.event.producer.ProductionProducer;
import com.aerospace.production_service.repository.ProductionOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionScheduler {

    private final ProductionOrderRepository orderRepository;
    private final ProductionProducer producer;

    /**
     * Runs daily at 06:00 AM.
     * Flags overdue orders (plannedEndDate passed, not COMPLETED/CANCELLED) → ON_HOLD.
     * Mirrors ExpiryScheduler pattern from inventory-service.
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void autoFlagOverdueOrders() {
        List<ProductionOrder> overdue = orderRepository.findOverdueOrders(LocalDateTime.now());

        for (ProductionOrder order : overdue) {
            // Only flag active orders — skip if already ON_HOLD
            if (order.getStatus() == ProductionOrderStatus.ON_HOLD) continue;

            log.warn("Auto-flagging overdue order: poNumber={}, plannedEnd={}",
                    order.getPoNumber(), order.getPlannedEndDate());

            order.setStatus(ProductionOrderStatus.ON_HOLD);
            order.setRemarks("Auto-flagged ON_HOLD by scheduler — planned end date passed");
            orderRepository.save(order);

            try {
                producer.sendProductionOrderOnHold(order.getId(), order.getPoNumber(),
                        "Overdue — auto-flagged by scheduler");
            } catch (Exception ex) {
                log.warn("[KAFKA] ON_HOLD publish failed for poNumber={}: {}",
                        order.getPoNumber(), ex.getMessage());
            }
        }

        log.info("Overdue scheduler completed. {} orders flagged ON_HOLD.", overdue.size());
    }

    /**
     * Runs every day at 07:00 AM.
     * Alerts on orders starting within the next 24 hours that are still PLANNED.
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void alertOrdersStartingSoon() {
        LocalDateTime now = LocalDateTime.now();
        List<ProductionOrder> upcoming = orderRepository.findOrdersStartingSoon(
                now, now.plusHours(24));

        for (ProductionOrder order : upcoming) {
            log.info("ALERT: Production order starting soon — poNumber={}, plannedStart={}",
                    order.getPoNumber(), order.getPlannedStartDate());
            // Future: publish a PRODUCTION_ORDER_DUE_SOON notification event here
        }

        log.info("Start-soon scheduler completed. {} orders starting within 24h.", upcoming.size());
    }
}
