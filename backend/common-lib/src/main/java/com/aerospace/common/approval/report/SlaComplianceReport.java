package com.aerospace.common.approval.report;

import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data @Builder
public class SlaComplianceReport {
    String moduleCode; LocalDateTime periodFrom; LocalDateTime periodTo;
    int compliantCount; int breachedCount; int noSlaConfiguredCount; Double compliancePercentage;
    List<SlaComplianceDetailRow> details;
}
