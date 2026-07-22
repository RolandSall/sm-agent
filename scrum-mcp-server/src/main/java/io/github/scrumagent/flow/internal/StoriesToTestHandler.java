package io.github.scrumagent.flow.internal;

import io.github.scrumagent.flow.StoriesToTestQuery;
import io.github.scrumagent.flow.StoriesToTestReport;
import io.github.scrumagent.flow.StoriesToTestReport.StoryToTest;
import io.github.scrumagent.jira.IssueSearch;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.shared.IssueFilters;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists dev-complete stories waiting for the PO to accept. Reuses the release-queue style
 * {@code issuesInStatuses} search (scoped across the configured projects, not one sprint), then maps
 * each issue to the ingredients a PO tests with (AC + epic).
 */
@QueryHandler(StoriesToTestQuery.class)
public class StoriesToTestHandler implements IQueryHandler<StoriesToTestQuery, StoriesToTestReport> {

    /**
     * Default "ready to test" status: the PO's acceptance queue on this instance is "Ready for
     * Acceptance" (confirmed for both projects). VAL also uses "Under Validation"/"Under Testing" —
     * those are a team call, passed as parameters, not baked in here.
     */
    private static final List<String> DEFAULT_STATUSES = List.of("Ready for Acceptance");

    private final IssueSearch jira;

    public StoriesToTestHandler(IssueSearch jira) {
        this.jira = jira;
    }

    @Override
    public StoriesToTestReport execute(StoriesToTestQuery query) {
        List<String> statuses = query.getStatuses();
        if (statuses == null || statuses.isEmpty()) {
            statuses = DEFAULT_STATUSES;
        }
        List<StoryToTest> stories = new ArrayList<>();
        for (JiraIssue issue : jira.issuesInStatuses(statuses)) {
            if (!IssueFilters.matches(query.getIssueType(), issue.type())
                    || !IssueFilters.matches(query.getAssignee(), issue.assignee())) {
                continue;
            }
            stories.add(new StoryToTest(issue.key(), issue.summary(), issue.assignee(),
                    issue.acceptanceCriteria(), issue.epicKey(), issue.status()));
        }
        return new StoriesToTestReport(stories);
    }
}
