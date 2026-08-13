package com.aerospace.reporting_service.client.dto;

import lombok.Data;

@Data
public class SalesReportSummaryDTO {
    private int periodDays;
    private long orderIntakeCount;
    private long openOrdersCount;
    private long backorderedOrdersCount;
    private double onTimeDeliveryPercent;
    private long dispatchedCount;
}