package io.github.scrumagent.jira.internal;

import io.github.scrumagent.jira.GetIssueQuery;
import io.github.scrumagent.jira.IssueLookup;
import io.github.scrumagent.jira.JiraIssue;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

@QueryHandler(GetIssueQuery.class)
public class GetIssueHandler implements IQueryHandler<GetIssueQuery, JiraIssue> {

    private final IssueLookup jira;

    public GetIssueHandler(IssueLookup jira) {
        this.jira = jira;
    }

    @Override
    public JiraIssue execute(GetIssueQuery query) {
        return jira.getIssue(query.getIssueKey());
    }
}
