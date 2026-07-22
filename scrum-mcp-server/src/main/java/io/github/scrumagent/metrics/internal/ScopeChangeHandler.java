package io.github.scrumagent.metrics.internal;

import io.github.scrumagent.jira.SprintQueries;
import io.github.scrumagent.jira.JiraSprint;
import io.github.scrumagent.jira.SprintIssue;
import io.github.scrumagent.metrics.ScopeChangeQuery;
import io.github.scrumagent.metrics.ScopeChangeReport;
import io.github.scrumagent.metrics.ScopeChangeReport.ScopeChangeItem;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Sums points committed-at-start / added / removed since the active sprint started, from the
 * changelog flags, and lists the issues behind each bucket. No verdict: whether the sprint grew,
 * shrank or churned is left to the sprint-metrics skill (see that SKILL.md).
 */
@QueryHandler(ScopeChangeQuery.class)
public class ScopeChangeHandler implements IQueryHandler<ScopeChangeQuery, ScopeChangeReport> {

    private final SprintQueries jira;

    public ScopeChangeHandler(SprintQueries jira) {
        this.jira = jira;
    }

    @Override
    public ScopeChangeReport execute(ScopeChangeQuery query) {
        JiraSprint sprint = jira.activeSprint();
        if (sprint == null) {
            return new ScopeChangeReport(null, 0, 0, 0, List.of(), List.of());
        }

        double committedAtStart = 0, addedPoints = 0, removedPoints = 0;
        List<ScopeChangeItem> added = new ArrayList<>();
        List<ScopeChangeItem> removed = new ArrayList<>();

        for (SprintIssue issue : jira.sprintIssues(sprint.id())) {
            double points = issue.storyPoints() == null ? 0 : issue.storyPoints();
            if (issue.removedFromSprint()) {
                removedPoints += points;
                removed.add(new ScopeChangeItem(issue.key(), points));
            } else if (issue.addedAfterSprintStart()) {
                addedPoints += points;
                added.add(new ScopeChangeItem(issue.key(), points));
            } else {
                committedAtStart += points;
            }
        }
        // removed issues were part of the original commitment too
        committedAtStart += removedPoints;

        return new ScopeChangeReport(sprint.name(), committedAtStart, addedPoints, removedPoints,
                added, removed);
    }
}
