package com.aerospace.production_resource_service.config;

import com.aerospace.production_resource_service.dto.MachineResponseDTO;
import com.aerospace.production_resource_service.event.MaintenanceDueEvent;
import com.aerospace.production_resource_service.event.producer.MaintenanceProducer;
import com.aerospace.production_resource_service.service.MachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceDueScheduler {

    private final MachineService machineService;
    private final MaintenanceProducer maintenanceProducer;

    @Value("${production.resource.maintenance.alert-window-days:30}")
    private int alertWindowDays;

    @Scheduled(cron = "0 0 4 * * *")
    public void alertDueOrOverdueMachines() {
        log.debug("Scheduler: checking for machines due/overdue for maintenance...");
        LocalDateTime now = LocalDateTime.now();

        for (MachineResponseDTO machine : machineService.getDueList(alertWindowDays)) {
            long daysUntilDue = Duration.between(now, machine.getNextMaintenanceDueAt()).toDays();
            maintenanceProducer.publishMaintenanceDue(MaintenanceDueEvent.builder()
                    .machineCode(machine.getMachineCode())
                    .machineName(machine.getName())
                    .machineType(machine.getMachineType())
                    .location(machine.getLocation())
                    .nextMaintenanceDueAt(machine.getNextMaintenanceDueAt())
                    .overdue(machine.isOverdue())
                    .daysUntilDue(daysUntilDue)
                    .build());
        }
    }
}
