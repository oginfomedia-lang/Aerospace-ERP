package com.aerospace.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;  // e.g., "STORE_KEEPER", "QA_INSPECTOR", "ADMIN"

    private String description;

    public Role(String name) {
        this.name = name;
    }
}