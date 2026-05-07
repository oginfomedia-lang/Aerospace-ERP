package com.aerospace.order_service.event.consumer;

import com.aerospace.order_service.entity.WorkOrderStatus;
import com.aerospace.order_service.repository.WorkOrderRepository;
import com.aerospace.order_service.repository.WorkOrderStatusHistoryRepository;
import com.aerospace.order_service.entity.WorkOrderStatusHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkOrderConsumer {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;

    /**
     * Listens to QA rejection events from inventory-service.
     * If any IN_PROGRESS work order uses the rejected batch,
     * it automatically moves to ON_HOLD so technicians are alerted.
     */
    @KafkaListener(
        topics   = "${workorder.kafka.topics.qa-status-changed}",
        groupId  = "workorder-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onQaStatusChanged(Map<String, Object> event) {
        try {
            String newStatus = String.valueOf(event.get("newStatus"));
            String batchNumber = String.valueOf(event.get("batchNumber"));
            String partName = String.valueOf(event.getOrDefault("partName", "Unknown"));

            log.info("[CONSUMER] QA_STATUS_CHANGED → batch={} status={}", batchNumber, newStatus);

            // Only act on rejections
            if (!"REJECTED".equalsIgnoreCase(newStatus)) return;

            // Find any IN_PROGRESS WOs that have this batch in their material lines
            workOrderRepository
                .findByStatusAndMaterialLineItems_BatchNumber(WorkOrderStatus.IN_PROGRESS, batchNumber)
                .forEach(wo -> {
                    WorkOrderStatus prev = wo.getStatus();
                    wo.setStatus(WorkOrderStatus.ON_HOLD);
                    wo.setRemarks("Auto-placed ON_HOLD: batch " + batchNumber
                            + " (" + partName + ") was QA REJECTED.");
                    workOrderRepository.save(wo);

                    historyRepository.save(WorkOrderStatusHistory.builder()
                            .workOrderId(wo.getId())
                            .woNumber(wo.getWoNumber())
                            .fromStatus(prev)
                            .toStatus(WorkOrderStatus.ON_HOLD)
                            .changedBy("System (QA Rejection)")
                            .remarks("Batch " + batchNumber + " rejected — WO auto-held")
                            .build());

                    log.info("[CONSUMER] WO {} moved IN_PROGRESS → ON_HOLD due to batch rejection",
                            wo.getWoNumber());
                });

        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to process QA status change: {}", ex.getMessage(), ex);
        }
    }
}