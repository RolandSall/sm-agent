package io.github.scrumagent.hygiene;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: hygiene report for the active sprint. Dispatched by the MCP tool and CLI. The
 * optional issue-type and assignee filters (Tier 2) narrow the examined set handler-side; a
 * {@code null}/blank filter matches all.
 */
public class BoardHygieneQuery implements IQuery<BoardHygieneReport> {

    private final String issueType;
    private final String assignee;

    public BoardHygieneQuery(String issueType, String assignee) {
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
