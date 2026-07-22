package io.github.scrumagent.jira;

/** One issue plus its status-transition timeline. */
public interface IssueHistoryQueries {

    /** One issue plus its status-transition timeline (for the cross-team dependency radar). */
    JiraIssueHistory getIssueHistory(String key);
}
