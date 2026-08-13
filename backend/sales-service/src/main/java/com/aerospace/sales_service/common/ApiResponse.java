package com.aerospace.sales_service.common;

import lombok.AllArgsConstructor;
import lombok.Data;

// Every API endpoint returns this wrapper — consistent with qa-service's pattern
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private String message;
    private T data;
}
