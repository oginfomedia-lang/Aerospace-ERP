package com.aerospace.inventory_service.scheduler;

import com.aerospace.common.sla.SlaEvaluator;
import com.aerospace.inventory_service.entity.CapaRecord;
import com.aerospace.inventory_service.entity.CapaStatus;
import com.aerospace.inventory_service.event.CapaEvent;
import com.aerospace.inventory_service.event.producer.InventoryProducer;
import com.aerospace.inventory_service.repository.CapaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Runs daily at 4 AM — staggered from ExpiryScheduler (2 AM) and CalibrationDueScheduler
// (3 AM), same reasoning those two already use to avoid competing for DB/Kafka at once.
// Fills the one real gap SLA research found: CapaRecord.targetCloseDate had no
// overdue-check anywhere in the codebase before this.
@Component
@RequiredArgsConstructor
@Slf4j
public class CapaOverdueScheduler {

    private static final List<CapaStatus> TERMINAL = List.of(CapaStatus.CLOSED, CapaStatus.REJECTED);

    private final CapaRepository capaRepository;
    private final InventoryProducer inventoryProducer;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void alertOverdueCapas() {
        LocalDateTime now = LocalDateTime.now();
        List<CapaRecord> overdue = capaRepository
                .findByTargetCloseDateBeforeAndOverdueNotifiedAtIsNullAndStatusNotIn(now, TERMINAL);

        for (CapaRecord capa : overdue) {
            var slaStatus = SlaEvaluator.evaluate(capa.getTargetCloseDate(), now);
            log.warn("[CAPA-OVERDUE] {} is {}h overdue (target close: {})",
                    capa.getCapaNumber(), slaStatus.hoursOverdue(), capa.getTargetCloseDate());

            capa.setOverdueNotifiedAt(now);
            capaRepository.save(capa);

            try {
                inventoryProducer.sendCapaEvent(CapaEvent.builder()
                        .id(capa.getId())
                        .capaNumber(capa.getCapaNumber())
                        .ncrId(capa.getNcr().getId())
                        .partName(null)
                        .capaType(capa.getCapaType().name())
                        .fromStatus(capa.getStatus().name())
                        .toStatus(capa.getStatus().name())
                        .actor("SYSTEM")
                        .remarks("Overdue — target close date " + capa.getTargetCloseDate() + " passed ("
                                + slaStatus.hoursOverdue() + "h ago)")
                        .overdue(true)
                        .timestamp(now)
                        .build());
            } catch (Exception ex) {
                log.warn("[KAFKA] CAPA_EVENT (overdue) publish failed for {}: {}", capa.getCapaNumber(), ex.getMessage());
            }
        }
        log.info("CAPA overdue scheduler completed. {} record(s) flagged.", overdue.size());
    }
}