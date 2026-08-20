package com.aerospace.finance_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgingBucketDTO {
    private String bucketLabel;
    private BigDecimal totalAmount;
    private Map<String, BigDecimal> byCounterparty;
}
