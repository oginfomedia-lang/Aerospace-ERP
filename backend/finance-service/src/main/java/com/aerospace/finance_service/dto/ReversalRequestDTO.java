package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReversalRequestDTO {

    @NotBlank(message = "A reason is required to post a reversing entry")
    private String reason;
}
