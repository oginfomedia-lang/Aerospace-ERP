package com.aerospace.qa_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FaiCharacteristicDTO {
    private Integer charNo;
    private String description;
    private String requirement;
    private String measuredResult;
    private String result; // PASS, FAIL
    private String remarks;
}
