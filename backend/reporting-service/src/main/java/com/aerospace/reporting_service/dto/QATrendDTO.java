package com.aerospace.reporting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QATrendDTO {
    private String decision;
    private Long count;
}
