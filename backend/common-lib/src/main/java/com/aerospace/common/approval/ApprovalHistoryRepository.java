package com.aerospace.common.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {

    List<ApprovalHistory> findByModuleCodeAndDocumentIdOrderByApprovalLevelAsc(String moduleCode, Long documentId);
    List<ApprovalHistory> findAllByStatusAndEscalatedFalse(ApprovalActionStatus status);
}