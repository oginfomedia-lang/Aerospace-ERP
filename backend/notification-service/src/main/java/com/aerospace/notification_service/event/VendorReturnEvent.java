package com.aerospace.notification_service.event;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorReturnEvent {
    private Long partId;
    private String skuCode;
    private String partName;
    private String batchNumber;
    private Integer quantity;
    private String ncrNumber;
    private String poNumber;
    private String dispositionedBy;
    private String remarks;
    private LocalDateTime dispositionedAt;
}
