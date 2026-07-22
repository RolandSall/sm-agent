package io.github.scrumagent.jira;

import java.util.List;

/** Bulk issue searches over the configured project/board. */
public interface IssueSearch {

    /** All issues in the active sprint of the configured project/board. */
    List<JiraIssue> activeSprintIssues();

    /** Project issues currently sitting in any of the given statuses (e.g. the release queue). */
    List<JiraIssue> issuesInStatuses(List<String> statuses);
}
