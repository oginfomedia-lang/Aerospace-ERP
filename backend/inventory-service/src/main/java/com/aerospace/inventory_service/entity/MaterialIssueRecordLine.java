package com.aerospace.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_issue_record_lines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialIssueRecordLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "material_issue_record_id")
    private MaterialIssueRecord materialIssueRecord;

    private String batchNumber;
    private Integer quantityTaken;
    private String location;
    private LocalDateTime expiryDate;
}
