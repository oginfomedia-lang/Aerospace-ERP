package com.aerospace.procurement_service.config;

import com.aerospace.common.approval.ApprovalMatrix;
import com.aerospace.common.approval.ApprovalMatrixRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// Seeds the default PURCHASE_ORDER approval bands (PUR-BR-001) on first boot, using the
// roles already defined in auth-service.RoleName. Amount bands are configurable afterwards
// via the approval_matrix table — this only runs when the table is empty.
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalMatrixSeeder implements CommandLineRunner {

    private static final String MODULE = "PURCHASE_ORDER";

    private final ApprovalMatrixRepository repository;

    @Override
    public void run(String... args) {
        if (repository.countByModuleCode(MODULE) > 0) {
            return;
        }

        repository.saveAll(List.of(
                ApprovalMatrix.builder().moduleCode(MODULE)
                        .amountFrom(BigDecimal.ZERO).amountTo(new BigDecimal("50000"))
                        .approverRole("PROCUREMENT_OFFICER").levelSequence(1).isActive(true).build(),
                ApprovalMatrix.builder().moduleCode(MODULE)
                        .amountFrom(new BigDecimal("50000.01")).amountTo(new BigDecimal("500000"))
                        .approverRole("MANAGER").levelSequence(2).isActive(true).build(),
                ApprovalMatrix.builder().moduleCode(MODULE)
                        .amountFrom(new BigDecimal("500000.01")).amountTo(new BigDecimal("999999999"))
                        .approverRole("ADMIN").levelSequence(3).isActive(true).build()
        ));

        log.info("Seeded default PURCHASE_ORDER approval matrix (3 levels).");
    }
}
