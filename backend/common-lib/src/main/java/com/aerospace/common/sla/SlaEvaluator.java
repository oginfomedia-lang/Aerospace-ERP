package com.aerospace.common.sla;

import java.time.Duration;
import java.time.LocalDateTime;

// One shared way to answer "is this overdue, by how much" — instead of every service
// hand-rolling its own isBefore(now) check slightly differently (as the 5 existing
// schedulers in this codebase currently do). Stateless, no DB — safe to call from
// anywhere, including inside a @Transactional method, with zero side effects.
public class SlaEvaluator {

    private SlaEvaluator() {}

    public static SlaStatus evaluate(LocalDateTime dueAt, LocalDateTime now) {
        if (dueAt == null) {
            return new SlaStatus(false, 0, 0);
        }
        boolean overdue = dueAt.isBefore(now);
        long hoursOverdue = overdue ? Duration.between(dueAt, now).toHours() : 0;
        long hoursUntilDue = overdue ? 0 : Duration.between(now, dueAt).toHours();
        return new SlaStatus(overdue, hoursOverdue, hoursUntilDue);
    }
}