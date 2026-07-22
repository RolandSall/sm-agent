package io.github.scrumagent.jira;

import io.github.springmediator.mediator.core.IQuery;

/** Mediator query: fetch one curated issue. Dispatched by MCP tools and the debug REST API. */
public class GetIssueQuery implements IQuery<JiraIssue> {

    private final String issueKey;

    public GetIssueQuery(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getIssueKey() {
        return issueKey;
    }
}
