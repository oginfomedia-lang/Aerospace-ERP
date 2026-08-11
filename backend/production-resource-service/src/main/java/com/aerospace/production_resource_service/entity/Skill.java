package com.aerospace.production_resource_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    // If true, an employee needs a non-expired EmployeeSkillCertification row (with a
    // certificate number) to count as eligible for this skill — not just a level.
    @Builder.Default
    private boolean certificationRequired = false;
}
