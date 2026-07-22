package io.github.scrumagent.dependencies.internal;

import io.github.scrumagent.dependencies.DependencyReport;
import io.github.scrumagent.dependencies.SprintDependenciesQuery;
import io.github.scrumagent.jira.DependencyQueries;
import io.github.scrumagent.jira.IssueSearch;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.jira.TeamDependency;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.List;

/**
 * Cross-team dependency radar across the active sprint: takes the distinct Epics of the sprint's
 * stories and fetches EVERY {@code Dependency}-type issue linked to those Epics — raw, unfiltered.
 * Ingredients only: no direction/resolved filter and no verdict. The agent applies the "open, our-team
 * waiting" rule from {@code ourTeam} and each row's status.
 */
@QueryHandler(SprintDependenciesQuery.class)
public class SprintDependenciesHandler
        implements IQueryHandler<SprintDependenciesQuery, DependencyReport> {

    private final IssueSearch issues;
    private final DependencyQueries dependencies;

    public SprintDependenciesHandler(IssueSearch issues, DependencyQueries dependencies) {
        this.issues = issues;
        this.dependencies = dependencies;
    }

    @Override
    public DependencyReport execute(SprintDependenciesQuery query) {
        List<String> epicKeys = issues.activeSprintIssues().stream()
                .map(JiraIssue::epicKey)
                .filter(k -> k != null && !k.isBlank())
                .distinct()
                .toList();
        List<TeamDependency> deps = dependencies.teamDependencies(epicKeys);
        return new DependencyReport("active-sprint", dependencies.teamName(), deps);
    }
}
