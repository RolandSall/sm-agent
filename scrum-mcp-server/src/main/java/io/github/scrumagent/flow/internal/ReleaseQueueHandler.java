package io.github.scrumagent.flow.internal;

import io.github.scrumagent.flow.ReleaseQueueQuery;
import io.github.scrumagent.flow.ReleaseQueueReport;
import io.github.scrumagent.flow.ReleaseQueueReport.ReleaseQueueItem;
import io.github.scrumagent.jira.IssueSearch;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.shared.IssueFilters;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Counts issues in the supplied release statuses, bucketed by status. */
@QueryHandler(ReleaseQueueQuery.class)
public class ReleaseQueueHandler implements IQueryHandler<ReleaseQueueQuery, ReleaseQueueReport> {

    private final IssueSearch jira;

    public ReleaseQueueHandler(IssueSearch jira) {
        this.jira = jira;
    }

    @Override
    public ReleaseQueueReport execute(ReleaseQueueQuery query) {
        List<String> statuses = query.getReleaseStatuses();
        if (statuses == null || statuses.isEmpty()) {
            return new ReleaseQueueReport(0, Map.of(), List.of());
        }

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        List<ReleaseQueueItem> items = new ArrayList<>();
        for (JiraIssue issue : jira.issuesInStatuses(statuses)) {
            if (!IssueFilters.matches(query.getIssueType(), issue.type())
                    || !IssueFilters.matches(query.getAssignee(), issue.assignee())) {
                continue;
            }
            byStatus.merge(issue.status(), 1, Integer::sum);
            items.add(new ReleaseQueueItem(issue.key(), issue.summary(), issue.status(), issue.assignee()));
        }
        // count reflects the filtered set (items.size()), so count == sum of byStatus in every case.
        return new ReleaseQueueReport(items.size(), byStatus, items);
    }
}
