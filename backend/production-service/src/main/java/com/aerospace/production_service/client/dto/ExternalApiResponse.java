package com.aerospace.production_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Matches production-resource-service's common/ApiResponse.java wrapper shape.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalApiResponse<T> {
    private String message;
    private T data;
}
