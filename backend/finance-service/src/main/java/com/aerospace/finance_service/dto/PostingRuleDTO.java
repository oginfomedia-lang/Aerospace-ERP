package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.PostingTriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostingRuleDTO {
    private Long id;

    @NotNull(message = "Trigger event is required")
    private PostingTriggerType triggerEvent;

    @NotBlank(message = "Debit account code is required")
    private String debitAccountCode;

    @NotBlank(message = "Credit account code is required")
    private String creditAccountCode;

    private String taxRuleCode;
    private Long costCenterId;
    private Long profitCenterId;
    private Boolean isActive;
}
