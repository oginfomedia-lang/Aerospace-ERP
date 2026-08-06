package com.aerospace.procurement_service.scheduler;

import com.aerospace.common.sla.SlaEvaluator;
import com.aerospace.procurement_service.entity.POStatus;
import com.aerospace.procurement_service.entity.PurchaseOrder;
import com.aerospace.procurement_service.event.PoDeliveryOverdueEvent;
import com.aerospace.procurement_service.event.producer.ProcurementEventPublisher;
import com.aerospace.procurement_service.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Runs daily at 4:30 AM — staggered from inventory-service's CapaOverdueScheduler (4 AM)
// so the two don't compete for DB/Kafka at the same instant. Fills the gap SLA research
// found: PurchaseOrder.deliveryDate had no overdue-check anywhere in the codebase.
@Component
@RequiredArgsConstructor
@Slf4j
public class PoDeliveryOverdueScheduler {

    private static final List<POStatus> IN_FLIGHT =
            List.of(POStatus.APPROVED, POStatus.SENT_TO_VENDOR, POStatus.PARTIALLY_RECEIVED);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProcurementEventPublisher eventPublisher;

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void alertOverduePurchaseOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<PurchaseOrder> overdue = purchaseOrderRepository
                .findByDeliveryDateBeforeAndOverdueNotifiedAtIsNullAndStatusIn(now, IN_FLIGHT);

        for (PurchaseOrder po : overdue) {
            var slaStatus = SlaEvaluator.evaluate(po.getDeliveryDate(), now);
            log.warn("[PO-OVERDUE] {} is {}h overdue (delivery date: {})",
                    po.getPoNumber(), slaStatus.hoursOverdue(), po.getDeliveryDate());

            po.setOverdueNotifiedAt(now);
            purchaseOrderRepository.save(po);

            try {
                eventPublisher.publishPoDeliveryOverdue(PoDeliveryOverdueEvent.builder()
                        .poId(po.getId())
                        .poNumber(po.getPoNumber())
                        .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : null)
                        .deliveryDate(po.getDeliveryDate())
                        .hoursOverdue(slaStatus.hoursOverdue())
                        .build());
            } catch (Exception ex) {
                log.warn("[KAFKA] PO_DELIVERY_OVERDUE publish failed for {}: {}", po.getPoNumber(), ex.getMessage());
            }
        }
        log.info("PO delivery-overdue scheduler completed. {} record(s) flagged.", overdue.size());
    }
}