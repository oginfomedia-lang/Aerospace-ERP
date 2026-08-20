package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.AccountDTO;
import com.aerospace.finance_service.entity.Account;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.AccountRepository;
import com.aerospace.finance_service.repository.JournalLineRepository;
import com.aerospace.finance_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository; // to check "ever posted to"

    @Override
    @Transactional
    public AccountDTO create(AccountDTO dto) {
        if (accountRepository.existsByAccountCode(dto.getAccountCode())) {
            throw new IllegalArgumentException("Account code already exists: " + dto.getAccountCode());
        }
        Account parent = dto.getParentAccountId() == null ? null :
                accountRepository.findById(dto.getParentAccountId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent account not found: " + dto.getParentAccountId()));
        Account entity = Account.builder()
                .accountCode(dto.getAccountCode()).accountName(dto.getAccountName())
                .accountType(dto.getAccountType()).parentAccount(parent)
                .financialCategory(dto.getFinancialCategory())
                .currencyRestrictions(dto.getCurrencyRestrictions())
                .postingPermission(dto.getPostingPermission() == null ? Account.PostingPermission.POSTABLE : dto.getPostingPermission())
                .activeStatus(dto.getActiveStatus() == null || dto.getActiveStatus())
                .build();
        return toDTO(accountRepository.save(entity));
    }

    @Override
    public AccountDTO getById(Long id) {
        return toDTO(accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id)));
    }

    @Override
    public List<AccountDTO> getAll() {
        return accountRepository.findAll().stream().map(this::toDTO).toList();
    }

    /** Builds the full hierarchical tree, root nodes down. */
    @Override
    public List<AccountDTO> getTree() {
        return accountRepository.findByParentAccountIsNull().stream().map(this::toTreeDTO).toList();
    }

    private AccountDTO toTreeDTO(Account node) {
        AccountDTO dto = toDTO(node);
        dto.setChildren(accountRepository.findByParentAccount_Id(node.getId()).stream()
                .map(this::toTreeDTO).toList());
        return dto;
    }

    @Override
    @Transactional
    public AccountDTO update(Long id, AccountDTO dto) {
        Account entity = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        entity.setAccountName(dto.getAccountName());
        entity.setFinancialCategory(dto.getFinancialCategory());
        entity.setCurrencyRestrictions(dto.getCurrencyRestrictions());
        if (dto.getPostingPermission() != null) entity.setPostingPermission(dto.getPostingPermission());
        if (dto.getParentAccountId() != null && !dto.getParentAccountId().equals(id)) {
            Account newParent = accountRepository.findById(dto.getParentAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent account not found: " + dto.getParentAccountId()));
            assertNoCycle(id, newParent);
            entity.setParentAccount(newParent);
        }
        return toDTO(accountRepository.save(entity));
    }

    private void assertNoCycle(Long accountId, Account candidateParent) {
        Account cursor = candidateParent;
        while (cursor != null) {
            if (cursor.getId().equals(accountId)) {
                throw new IllegalArgumentException("Cannot reparent account under its own descendant");
            }
            cursor = cursor.getParentAccount();
        }
    }

    /**
     * Deactivate-only guard: refuse if it has active children, or if it already has
     * journal postings. There is no hard delete once an account is created — exactly
     * like Chart of Accounts is treated in every real accounting system.
     */
    @Override
    @Transactional
    public void deactivate(Long id) {
        Account entity = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        if (accountRepository.existsByParentAccount_Id(id)) {
            throw new IllegalStateException("Cannot deactivate account with child accounts: " + entity.getAccountCode());
        }
        if (journalLineRepository.existsByAccountCode(entity.getAccountCode())) {
            throw new IllegalStateException("Cannot deactivate account with existing postings: " + entity.getAccountCode());
        }
        entity.setActiveStatus(false);
        accountRepository.save(entity);
    }

    private AccountDTO toDTO(Account e) {
        return AccountDTO.builder()
                .id(e.getId()).accountCode(e.getAccountCode()).accountName(e.getAccountName())
                .accountType(e.getAccountType())
                .parentAccountId(e.getParentAccount() == null ? null : e.getParentAccount().getId())
                .parentAccountCode(e.getParentAccount() == null ? null : e.getParentAccount().getAccountCode())
                .financialCategory(e.getFinancialCategory()).activeStatus(e.getActiveStatus())
                .currencyRestrictions(e.getCurrencyRestrictions()).postingPermission(e.getPostingPermission())
                .build();
    }
}
