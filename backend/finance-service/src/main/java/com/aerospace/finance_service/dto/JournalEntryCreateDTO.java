package com.aerospace.finance_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntryCreateDTO {

    @NotNull(message = "Posting date is required")
    private LocalDate postingDate;

    private String narration;
    private String sourceDocType;
    private String sourceDocRef;
    private Long costCenterId;
    private Long profitCenterId;
    private Long organizationId;
    private Long plantId;

    @NotEmpty(message = "A journal entry needs at least two lines")
    @Valid
    private List<JournalLineCreateDTO> lines;
}
