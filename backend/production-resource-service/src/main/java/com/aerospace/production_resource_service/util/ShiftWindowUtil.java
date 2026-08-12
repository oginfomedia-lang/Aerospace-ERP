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
    // BETWEEN start and end, on a listed day. An overnight shift's early-morning portion
    // (e.g. 00:00-06:00) falls on the calendar day AFTER the listed day — a shift listing
    // only "MON" with hours 22:00-06:00 must still cover Tuesday 03:00, since that's still
    // Monday night's shift. So the overnight case checks BOTH: today listed AND t >= start,
    // OR yesterday listed AND t <= end.
    public static boolean isWithin(Shift shift, LocalDateTime instant) {
        LocalTime t = instant.toLocalTime();
        LocalTime start = shift.getStartTime();
        LocalTime end = shift.getEndTime();

        if (start.isBefore(end) || start.equals(end)) {
            return dayMatches(shift, instant) && !t.isBefore(start) && !t.isAfter(end);
        }

        boolean eveningPortion = dayMatches(shift, instant) && !t.isBefore(start);
        boolean morningPortion = dayMatches(shift, instant.minusDays(1)) && !t.isAfter(end);
        return eveningPortion || morningPortion;
    }

    private static boolean dayMatches(Shift shift, LocalDateTime instant) {
        String day = instant.getDayOfWeek().name().substring(0, 3);
        return Arrays.stream(shift.getDaysOfWeek().split(","))
                .map(String::trim).anyMatch(d -> d.equalsIgnoreCase(day));
    }
}
