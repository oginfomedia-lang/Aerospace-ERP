package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxCategoryDTO {
    private Long id;

    @NotBlank(message = "Category code is required")
    private String categoryCode;

    @NotBlank(message = "Category name is required")
    private String categoryName;

    private Boolean activeStatus;
}
