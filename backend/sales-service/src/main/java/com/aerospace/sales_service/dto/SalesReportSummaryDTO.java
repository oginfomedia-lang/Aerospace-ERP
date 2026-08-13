package com.aerospace.sales_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalesReportSummaryDTO {
    private int periodDays;
    private long orderIntakeCount;        // orders created in the period
    private long openOrdersCount;         // CONFIRMED or ALLOCATED right now
    private long backorderedOrdersCount;  // of the open ones, short on allocated stock
    private double onTimeDeliveryPercent; // dispatched-in-period orders shipped on/before promise date
    private long dispatchedCount;
}
