package com.aerospace.common.approval.report;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/approval-reports")
@RequiredArgsConstructor
public class ApprovalReportController {
    private final ApprovalReportService reportService;

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/pending-approvals")
    public ResponseEntity<List<PendingApprovalRow>> pendingApprovals(@RequestParam(required = false) String moduleCode) {
        return ResponseEntity.ok(reportService.getPendingApprovals(moduleCode));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/sla-compliance")
    public ResponseEntity<SlaComplianceReport> slaCompliance(
            @RequestParam(required = false) String moduleCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reportService.getSlaCompliance(moduleCode, from, to));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/turnaround-time")
    public ResponseEntity<List<TurnaroundRow>> turnaroundTime(
            @RequestParam(required = false) String moduleCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reportService.getTurnaroundTimes(moduleCode, from, to));
    }
}
