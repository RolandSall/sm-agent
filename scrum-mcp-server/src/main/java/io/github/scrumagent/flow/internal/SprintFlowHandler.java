package io.github.scrumagent.flow.internal;

import io.github.scrumagent.flow.SprintFlowQuery;
import io.github.scrumagent.flow.SprintFlowReport;
import io.github.scrumagent.flow.SprintFlowReport.FlowIssue;
import io.github.scrumagent.jira.SprintFlowQueries;
import io.github.scrumagent.jira.SprintIssueFlow;
import io.github.scrumagent.shared.IssueFilters;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.List;

/**
 * Sprint-flow is a FETCH, not a verdict. It returns every active-sprint issue with its raw,
 * changelog-derived flow ingredients — statusCategory, ageInStatusDays, daysSinceUpdated,
 * reopenCount — as one flat list. It does NOT bucket into stuck / stalled / reopened and applies no
 * thresholds; those are team lines the agent applies (see the sprint-flow skill). The only logic
 * here is the fetch, the optional issue-type/assignee scoping, and passing the changelog-derived
 * arithmetic through verbatim.
 */
@QueryHandler(SprintFlowQuery.class)
public class SprintFlowHandler implements IQueryHandler<SprintFlowQuery, SprintFlowReport> {

    private final SprintFlowQueries jira;

    public SprintFlowHandler(SprintFlowQueries jira) {
        this.jira = jira;
    }

    @Override
    public SprintFlowReport execute(SprintFlowQuery query) {
        List<FlowIssue> issues = jira.activeSprintFlow().stream()
                .filter(f -> IssueFilters.matches(query.getIssueType(), f.type())
                        && IssueFilters.matches(query.getAssignee(), f.assignee()))
                .map(SprintFlowHandler::toFlowIssue)
                .toList();
        return new SprintFlowReport(issues);
    }

    private static FlowIssue toFlowIssue(SprintIssueFlow f) {
        return new FlowIssue(f.key(), f.summary(), f.assignee(), f.statusCategory(), f.status(),
                f.ageInStatusDays(), f.daysSinceUpdated(), f.reopenCount());
    }
}
