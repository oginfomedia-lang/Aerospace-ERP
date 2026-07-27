package com.aerospace.auth_service.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

public class SupplierAuthDTOs {

    @Data
    public static class SupplierInviteRequest {
        @NotNull private Long supplierId;
        @NotBlank private String email;
        @NotBlank private String supplierName; //for the invite emails greeting
    }

    @Data
    public static class SupplierActivateRequest {
        @NotBlank private String token;
        @NotBlank private String password;
    }

    @Data
    public static class SupplierResendInviteRequest {
        @NotBlank private String email;
        @NotBlank private String supplierName; //for the invite emails greeting
    }
}
