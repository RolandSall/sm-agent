package io.github.scrumagent.shared;

/**
 * Converts an hours quantity into working-days, given a working-hours-per-day figure. Days are a
 * DERIVED ingredient — the underlying hours math is never altered; this only re-expresses it so a
 * caller can reason in whole/partial days ("about two days of work") without doing the division.
 */
public final class WorkingDays {

    private WorkingDays() {
    }

    /**
     * {@code hours / workingHoursPerDay}, rounded to one decimal (nearest 0.1 of a day). A
     * non-positive {@code workingHoursPerDay} is treated as "unknown" and yields 0 rather than a
     * division blow-up: e.g. {@code fromHours(20, 8) == 2.5}, {@code fromHours(10.4, 8) == 1.3}.
     */
    public static double fromHours(double hours, double workingHoursPerDay) {
        if (workingHoursPerDay <= 0) {
            return 0.0;
        }
        return Math.round((hours / workingHoursPerDay) * 10.0) / 10.0;
    }
}
