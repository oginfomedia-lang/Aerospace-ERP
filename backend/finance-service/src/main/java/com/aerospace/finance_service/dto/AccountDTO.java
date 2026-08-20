package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.Account.AccountType;
import com.aerospace.finance_service.entity.Account.PostingPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountDTO {
    private Long id;

    @NotBlank(message = "Account code is required")
    private String accountCode;

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    private Long parentAccountId;
    private String parentAccountCode; // read-only convenience field
    private String financialCategory;
    private Boolean activeStatus;
    private String currencyRestrictions;
    private PostingPermission postingPermission;

    // populated only by the /tree endpoint
    private List<AccountDTO> children;
}
