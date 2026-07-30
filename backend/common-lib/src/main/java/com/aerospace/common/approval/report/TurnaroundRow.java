package com.aerospace.common.approval.report;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TurnaroundRow {
    String moduleCode;
    Long documentId;
    int documentVersion;
    String outcome;
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    long turnaroundHours;
    int levelsInChain;
}
