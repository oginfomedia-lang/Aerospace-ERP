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
}
