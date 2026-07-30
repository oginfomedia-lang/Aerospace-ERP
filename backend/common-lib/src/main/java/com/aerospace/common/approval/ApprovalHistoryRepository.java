package com.aerospace.common.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {

    List<ApprovalHistory> findByModuleCodeAndDocumentIdOrderByApprovalLevelAsc(String moduleCode, Long documentId);
    List<ApprovalHistory> findAllByStatusAndEscalatedFalse(ApprovalActionStatus status);
    List<ApprovalHistory> findByStatusOrderByPendingSinceAsc(ApprovalActionStatus status);
List<ApprovalHistory> findByModuleCodeAndStatusOrderByPendingSinceAsc(String moduleCode, ApprovalActionStatus status);
List<ApprovalHistory> findByStatusInAndApprovedAtBetween(List<ApprovalActionStatus> statuses, LocalDateTime from, LocalDateTime to);
List<ApprovalHistory> findByModuleCodeAndStatusInAndApprovedAtBetween(String moduleCode, List<ApprovalActionStatus> statuses, LocalDateTime from, LocalDateTime to);
List<ApprovalHistory> findByModuleCodeAndDocumentIdAndDocumentVersionOrderByApprovalLevelAsc(String moduleCode, Long documentId, int documentVersion);
}