package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.PostingTriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ManualPostingRequestDTO {

    @NotNull(message = "Trigger event is required")
    private PostingTriggerType triggerEvent;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String sourceDocType;
    private String sourceDocRef;
    private Long costCenterId;
    private Long profitCenterId;
}
