package io.github.scrumagent.flow;

import io.github.springmediator.mediator.core.IQuery;

import java.util.List;

/**
 * Mediator query: the PO-facing "ready to test" report — dev-complete stories waiting for acceptance,
 * across the configured projects (not sprint-limited: a PO tests dev-complete work whenever it is
 * ready). The status names are agent-supplied (Tier 2) and optional — a {@code null}/empty list falls
 * back to the handler's default ({@code Ready for Acceptance}).
 */
public class StoriesToTestQuery implements IQuery<StoriesToTestReport> {

    private final List<String> statuses;
    private final String issueType;
    private final String assignee;

    public StoriesToTestQuery(List<String> statuses, String issueType, String assignee) {
        this.statuses = statuses;
        this.issueType = issueType;
        this.assignee = assignee;
    }

    public List<String> getStatuses() {
        return statuses;
    }

    /** Optional issue-type filter ({@code null}/blank = all types). */
    public String getIssueType() {
        return issueType;
    }

    /** Optional assignee filter ({@code null}/blank = all assignees). */
    public String getAssignee() {
        return assignee;
    }
}
