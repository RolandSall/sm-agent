package io.github.scrumagent.worklog;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: logged-vs-estimate rollup for the active sprint. The optional issue-type and
 * assignee filters (Tier 2) narrow the rollup handler-side; a {@code null}/blank filter matches all.
 * The optional {@code hoursPerDay} (Tier 2) sets the divisor for the derived working-days figures;
 * a {@code null}/non-positive value falls back to the configured working-hours-per-day default.
 */
public class WorkloadQuery implements IQuery<WorkloadReport> {

    private final String issueType;
    private final String assignee;
    private final Double hoursPerDay;

    public WorkloadQuery(String issueType, String assignee, Double hoursPerDay) {
        this.issueType = issueType;
        this.assignee = assignee;
        this.hoursPerDay = hoursPerDay;
    }

    /** Optional issue-type filter ({@code null}/blank = all types). */
    public String getIssueType() {
        return issueType;
    }

    /** Optional assignee filter ({@code null}/blank = all assignees). */
    public String getAssignee() {
        return assignee;
    }

    /** Optional working-hours-per-day divisor for derived days ({@code null}/&le;0 = configured default). */
    public Double getHoursPerDay() {
        return hoursPerDay;
    }
}
