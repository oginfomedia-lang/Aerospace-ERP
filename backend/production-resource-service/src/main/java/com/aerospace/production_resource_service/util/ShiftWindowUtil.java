package com.aerospace.production_resource_service.util;

import com.aerospace.production_resource_service.entity.Shift;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

// Shared by EmployeeServiceImpl.occupyEmployee (checks "now") and
// EmployeeReservationServiceImpl (checks a future reservedFrom/reservedTo) — kept in
// one place so the overnight-shift wraparound logic can't drift between the two.
public final class ShiftWindowUtil {

    private ShiftWindowUtil() {
    }

    // Handles overnight shifts (e.g. 22:00-06:00): a same-day shift needs `instant`
    // BETWEEN start and end; an overnight one needs `instant` AFTER start OR BEFORE end.
    public static boolean isWithin(Shift shift, LocalDateTime instant) {
        String day = instant.getDayOfWeek().name().substring(0, 3);
        boolean dayMatches = Arrays.stream(shift.getDaysOfWeek().split(","))
                .map(String::trim).anyMatch(d -> d.equalsIgnoreCase(day));
        if (!dayMatches) return false;

        LocalTime t = instant.toLocalTime();
        LocalTime start = shift.getStartTime();
        LocalTime end = shift.getEndTime();
        if (start.isBefore(end) || start.equals(end)) {
            return !t.isBefore(start) && !t.isAfter(end);
        }
        return !t.isBefore(start) || !t.isAfter(end);
    }
}
