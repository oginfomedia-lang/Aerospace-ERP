package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.AccountDTO;

import java.util.List;

public interface AccountService {
    AccountDTO create(AccountDTO dto);
    AccountDTO getById(Long id);
    List<AccountDTO> getAll();
    List<AccountDTO> getTree();
    AccountDTO update(Long id, AccountDTO dto);
    void deactivate(Long id);
}
