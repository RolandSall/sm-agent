package io.github.scrumagent.flow;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: the daily-standup flow fetch. Returns every active-sprint issue with its raw
 * flow ingredients — no thresholds, no buckets. The stuck / stalled / reopened rules live in the
 * sprint-flow skill and are applied by the agent, not here. The only inputs are the optional
 * issue-type / assignee scoping filters.
 */
public class SprintFlowQuery implements IQuery<SprintFlowReport> {

    private final String issueType;
    private final String assignee;

    public SprintFlowQuery(String issueType, String assignee) {
        this.issueType = issueType;
        this.assignee = assignee;
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
