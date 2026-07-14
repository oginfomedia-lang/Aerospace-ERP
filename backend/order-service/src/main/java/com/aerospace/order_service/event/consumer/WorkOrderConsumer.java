package com.aerospace.order_service.event.consumer;

import com.aerospace.order_service.dto.WorkOrderDTOs.IssueMaterialRequest;
import com.aerospace.order_service.entity.WorkOrderStatus;
import com.aerospace.order_service.repository.WorkOrderRepository;
import com.aerospace.order_service.repository.WorkOrderStatusHistoryRepository;
import com.aerospace.order_service.entity.WorkOrderStatusHistory;
import com.aerospace.order_service.service.WorkOrderService;
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
    private final WorkOrderService workOrderService;

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

    /**
     * Listens to MATERIAL_ISSUED events from inventory-service. Inventory's stock
     * deduction is the source of truth for "material was issued" — when the issue
     * was made against a specific WO line (workOrderId + lineItemId present), this
     * is what actually updates that line's quantityIssued/status here, rather than
     * the frontend making a second, independent write that could drift out of sync
     * with the real stock deduction.
     */
    @KafkaListener(
        topics   = "${workorder.kafka.topics.material-issued}",
        groupId  = "workorder-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMaterialIssued(Map<String, Object> event) {
        try {
            Long workOrderId = asLong(event.get("workOrderId"));
            Long lineItemId = asLong(event.get("lineItemId"));
            if (workOrderId == null || lineItemId == null) {
                return; // ad-hoc issue, not tied to a work order line
            }

            Long quantityIssuedLong = asLong(event.get("quantityIssued"));
            Integer quantityIssued = quantityIssuedLong != null ? quantityIssuedLong.intValue() : null;
            String batchNumber = String.valueOf(event.getOrDefault("batchNumber", ""));
            String issuedBy = String.valueOf(event.getOrDefault("issuedBy", "System"));

            workOrderService.issueMaterial(workOrderId, IssueMaterialRequest.builder()
                    .lineItemId(lineItemId)
                    .quantityIssued(quantityIssued)
                    .batchNumber(batchNumber)
                    .issuedBy(issuedBy)
                    .build());

            log.info("[CONSUMER] MATERIAL_ISSUED → WO={} lineItemId={} qty={} batch={} — line updated",
                    workOrderId, lineItemId, quantityIssued, batchNumber);
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to process material issued event: {}", ex.getMessage(), ex);
        }
    }

    private Long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}