package io.github.scrumagent.flow;

import io.github.springmediator.mediator.core.IQuery;

import java.util.List;

/**
 * Mediator query: count issues sitting in the team's release / ready-to-test statuses. The status
 * names are agent-supplied (Tier 2) — Jira has no native category for a "ready to test" bucket.
 */
public class ReleaseQueueQuery implements IQuery<ReleaseQueueReport> {

    private final List<String> releaseStatuses;
    private final String issueType;
    private final String assignee;

    public ReleaseQueueQuery(List<String> releaseStatuses, String issueType, String assignee) {
        this.releaseStatuses = releaseStatuses;
        this.issueType = issueType;
        this.assignee = assignee;
    }

    public List<String> getReleaseStatuses() {
        return releaseStatuses;
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
