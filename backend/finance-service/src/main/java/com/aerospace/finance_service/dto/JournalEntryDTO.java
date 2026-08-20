package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.JournalEntry.JournalStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntryDTO {
    private Long id;
    private String journalNumber;
    private LocalDate postingDate;
    private String narration;
    private String sourceDocType;
    private String sourceDocRef;
    private JournalStatus status;
    private String createdBy;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime postedAt;
    private Long reversalOfJournalId;
    private Long reversedByJournalId;
    private List<JournalLineDTO> lines;
}
