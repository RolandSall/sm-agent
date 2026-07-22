package io.github.scrumagent.flow.internal;

import io.github.scrumagent.flow.WipReport;
import io.github.scrumagent.flow.WipReport.AssigneeWip;
import io.github.scrumagent.flow.WipStatusQuery;
import io.github.scrumagent.jira.IssueSearch;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.shared.IssueFilters;
import io.github.scrumagent.shared.StatusCategory;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts in-progress work (via {@code statusCategory}) per assignee and for the team. This is a
 * COUNT, not a verdict: it returns the exact per-assignee and team in-progress counts and echoes the
 * supplied limits as context. It never derives an over-limit flag — the agent compares counts to
 * limits (see the wip-limits skill).
 */
@QueryHandler(WipStatusQuery.class)
public class WipStatusHandler implements IQueryHandler<WipStatusQuery, WipReport> {

    private static final String UNASSIGNED = "Unassigned";

    private final IssueSearch jira;

    public WipStatusHandler(IssueSearch jira) {
        this.jira = jira;
    }

    @Override
    public WipReport execute(WipStatusQuery query) {
        Map<String, Integer> byAssignee = new LinkedHashMap<>();
        int teamInProgress = 0;

        for (JiraIssue issue : jira.activeSprintIssues()) {
            if (!IssueFilters.matches(query.getIssueType(), issue.type())
                    || !IssueFilters.matches(query.getAssignee(), issue.assignee())) {
                continue;
            }
            if (!StatusCategory.IN_PROGRESS.equalsIgnoreCase(issue.statusCategory())) {
                continue;
            }
            teamInProgress++;
            String assignee = issue.assignee() == null || issue.assignee().isBlank()
                    ? UNASSIGNED : issue.assignee();
            byAssignee.merge(assignee, 1, Integer::sum);
        }

        Integer perLimit = query.getPerAssigneeLimit();
        List<AssigneeWip> perAssignee = new ArrayList<>();
        for (Map.Entry<String, Integer> e : byAssignee.entrySet()) {
            // Echo the supplied per-assignee limit as context; never derive an over-limit flag.
            perAssignee.add(new AssigneeWip(e.getKey(), e.getValue(), perLimit));
        }

        // Echo the supplied team limit as context; never derive an over-limit flag.
        return new WipReport(teamInProgress, query.getTeamLimit(), perAssignee);
    }
}
