package com.aerospace.qa_service.config;

import com.aerospace.qa_service.dto.EquipmentResponseDTO;
import com.aerospace.qa_service.event.CalibrationDueEvent;
import com.aerospace.qa_service.event.producer.CalibrationProducer;
import com.aerospace.qa_service.service.CalibrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

// Runs daily at 3 AM — staggered from inventory-service's 2 AM expiry-check job so the
// two don't compete for DB/Kafka at the same instant.
@Component
@RequiredArgsConstructor
@Slf4j
public class CalibrationDueScheduler {

    private final CalibrationService calibrationService;
    private final CalibrationProducer calibrationProducer;

    @Value("${qa.calibration.alert-window-days:30}")
    private int alertWindowDays;

    @Scheduled(cron = "0 0 3 * * *")
    public void alertDueOrOverdueEquipment() {
        log.debug("Scheduler: checking for equipment due/overdue for calibration...");
        LocalDateTime now = LocalDateTime.now();

        for (EquipmentResponseDTO equipment : calibrationService.getDueList(alertWindowDays)) {
            long daysUntilDue = Duration.between(now, equipment.getNextCalibrationDueAt()).toDays();
            calibrationProducer.publishCalibrationDue(CalibrationDueEvent.builder()
                    .assetTag(equipment.getAssetTag())
                    .equipmentName(equipment.getName())
                    .equipmentType(equipment.getEquipmentType())
                    .location(equipment.getLocation())
                    .nextCalibrationDueAt(equipment.getNextCalibrationDueAt())
                    .overdue(equipment.isOverdue())
                    .daysUntilDue(daysUntilDue)
                    .build());
        }
    }
}
