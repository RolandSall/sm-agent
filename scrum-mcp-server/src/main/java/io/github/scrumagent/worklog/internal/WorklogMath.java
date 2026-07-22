package io.github.scrumagent.worklog.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tiny shared arithmetic for the worklog handlers — one home for the missing-value coercion. */
final class WorklogMath {

    private static final Logger log = LoggerFactory.getLogger(WorklogMath.class);

    private WorklogMath() {
    }

    /** A missing (null) hours value counts as zero: no log usually means nobody logged, not no work. */
    static double orZero(Double value) {
        return value == null ? 0.0 : value;
    }

    /**
     * Logged work can never be negative; a negative value is bad data (a botched worklog edit, a
     * corrupt sync). Clamp it to 0 and warn — a negative would otherwise poison every sum, ratio and
     * derived day figure downstream. Non-negative values pass through untouched.
     */
    static double clampLogged(String key, double value) {
        if (value < 0) {
            log.warn("Negative logged hours ({}) for {} — clamping to 0", value, key);
            return 0.0;
        }
        return value;
    }
}
