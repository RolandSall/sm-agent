package io.github.scrumagent.flow;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: WIP status for the active sprint. Limits are agent-supplied (Tier 2); a
 * {@code null} limit means "report the count but do not flag".
 */
public class WipStatusQuery implements IQuery<WipReport> {

    private final Integer perAssigneeLimit;
    private final Integer teamLimit;
    private final String issueType;
    private final String assignee;

    public WipStatusQuery(Integer perAssigneeLimit, Integer teamLimit, String issueType, String assignee) {
        this.perAssigneeLimit = perAssigneeLimit;
        this.teamLimit = teamLimit;
        this.issueType = issueType;
        this.assignee = assignee;
    }

    public Integer getPerAssigneeLimit() {
        return perAssigneeLimit;
    }

    public Integer getTeamLimit() {
        return teamLimit;
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
