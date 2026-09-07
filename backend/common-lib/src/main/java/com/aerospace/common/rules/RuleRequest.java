package com.aerospace.common.rules;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Request DTO for Rule create/update — kept separate from the @Entity so the request body
// can never bind straight onto persistence fields (id, isActive defaults, etc.) and so it
// carries real validation, unlike the entity it used to bind directly onto.
@Data
public class RuleRequest {
    @NotNull @Size(min = 1, max = 100)
    private String moduleCode;

    @NotNull @Size(min = 1, max = 100)
    private String fieldName;

    @NotNull
    private RuleOperator operator;

    @NotNull @Size(min = 1, max = 500)
    private String value;

    @NotNull
    private RuleAction action;

    @NotNull @Size(min = 1, max = 100)
    private String actionValue;

    private boolean isActive = true;
}
