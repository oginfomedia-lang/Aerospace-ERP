package com.aerospace.production_resource_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

// A shift definition — e.g. "Morning" 06:00-14:00, Mon-Fri. Pure lookup data, same
// shape as Skill (no history, no occupancy of its own — an Employee just points at
// one via Employee.shiftId).
@Entity
@Table(name = "shifts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    // Comma-separated 3-letter day codes, e.g. "MON,TUE,WED,THU,FRI" — matched against
    // LocalDate.now().getDayOfWeek().name().substring(0,3) at occupy-time.
    @Column(nullable = false)
    private String daysOfWeek;
}
