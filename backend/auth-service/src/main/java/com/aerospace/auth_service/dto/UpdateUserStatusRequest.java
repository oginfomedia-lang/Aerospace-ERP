package com.aerospace.auth_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class UpdateUserStatusRequest {
    @NotNull
    private Boolean enabled;
}
