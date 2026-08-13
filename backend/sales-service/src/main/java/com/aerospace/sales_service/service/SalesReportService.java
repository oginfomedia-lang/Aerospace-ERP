package com.aerospace.sales_service.service;

import com.aerospace.sales_service.dto.SalesReportSummaryDTO;

public interface SalesReportService {
    SalesReportSummaryDTO getSummary(int periodDays);
}
