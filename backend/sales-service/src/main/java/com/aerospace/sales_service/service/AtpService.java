package com.aerospace.sales_service.service;

import com.aerospace.sales_service.dto.AtpResultDTO;

// Available-to-Promise: "can we ship this now, and if not, when can we?" (§18.8).
public interface AtpService {
    AtpResultDTO evaluate(Long salesOrderLineId, Long partId, String partNumber, Integer requestedQuantity);
}
