package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapaEvent {
    private Long id;
    private String capaNumber;
    private Long ncrId;
    private String ncrNumber;
    private String partName;
    private String batchNumber;
    private String capaType;
    private String fromStatus;
    private String toStatus;
    private String actor;
    private String remarks;
    private LocalDateTime timestamp;
    // true only for the new daily overdue check below — false/absent for every normal
    // status-transition event this class already carries (fromStatus/toStatus stay
    // meaningful in both cases; overdue publishes with fromStatus == toStatus).
    @Builder.Default
    private boolean overdue = false;
}
