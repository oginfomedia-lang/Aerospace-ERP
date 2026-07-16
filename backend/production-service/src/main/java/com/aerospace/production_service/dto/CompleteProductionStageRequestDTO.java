package com.aerospace.production_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

// A PRODUCTION-type stage (e.g. Cutting, Drilling) just reports how much it made —
// the accept/reject decision belongs to the QA_INSPECTION stage that follows, not here.
@Data
public class CompleteProductionStageRequestDTO {
    @NotNull
    private Integer quantityProduced;
    private String operatorName;
    private String remarks;
}
