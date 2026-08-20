package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountCode(String accountCode);
    boolean existsByAccountCode(String accountCode);
    List<Account> findByParentAccountIsNull();               // tree roots
    List<Account> findByParentAccount_Id(Long parentAccountId); // children of a node
    boolean existsByParentAccount_Id(Long parentAccountId);
}
