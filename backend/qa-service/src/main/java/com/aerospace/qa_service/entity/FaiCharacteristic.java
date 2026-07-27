package com.aerospace.qa_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fai_characteristic")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaiCharacteristic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fai_record_id", nullable = false)
    private FaiRecord faiRecord;

    private Integer charNo; // drawing balloon number

    private String description;
    private String requirement;     // e.g. "150.00 +/- 0.05 mm"
    private String measuredResult;

    @Enumerated(EnumType.STRING)
    private CharacteristicResult result;

    private String remarks;
}
