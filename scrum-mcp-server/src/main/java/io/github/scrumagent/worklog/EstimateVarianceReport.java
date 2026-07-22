package io.github.scrumagent.worklog;

import java.util.List;

/**
 * Estimate-variance result: one entry per estimated active-sprint issue (those with an original
 * estimate at or above {@code minEstimateHours}), so the caller can apply its own line rather than
 * seeing only pre-flagged ones. It returns the raw INGREDIENTS and computes no verdict — the
 * OVERRUN / FINISHED_EARLY / NO_LOGGING judgement lives in the estimate-variance skill. All hours;
 * missing logged hours count as zero.
 *
 * @param issues one entry per estimated issue (on-track issues included, unflagged)
 */
public record EstimateVarianceReport(List<IssueVariance> issues) {

    /**
     * One estimated issue with its raw ingredients — no advisory, no verdict.
     *
     * <p>{@code loggedDays} / {@code originalEstimateDays} are DERIVED from the same hours via
     * {@code WorkingDays.fromHours}; the hours are authoritative and {@code usageRatio} is still
     * computed from hours, not days.
     *
     * <p>{@code usageRatio} (logged / originalEstimate, 0 when the estimate is 0) and
     * {@code statusCategory} (Jira's built-in bucket, passed through raw) are the deterministic
     * ingredients — the caller decides what counts as a problem.
     */
    public record IssueVariance(
            String key,
            String assignee,
            double loggedHours,
            double originalEstimateHours,
            double loggedDays,
            double originalEstimateDays,
            double usageRatio,
            String statusCategory) {
    }
}
