package io.github.scrumagent.dependencies.internal;

import io.github.scrumagent.dependencies.DependenciesOfQuery;
import io.github.scrumagent.dependencies.DependencyReport;
import io.github.scrumagent.jira.DependencyQueries;
import io.github.scrumagent.jira.IssueLookup;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.jira.TeamDependency;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.List;

/**
 * Cross-team dependency radar for a single issue: resolves the issue's Epic and fetches EVERY
 * {@code Dependency}-type issue linked to it — raw, unfiltered. No verdict, no direction/resolved
 * filter; that reasoning is the agent's, from {@code ourTeam} and each row's status. An issue with no
 * Epic has no Dependencies to report.
 */
@QueryHandler(DependenciesOfQuery.class)
public class DependenciesOfHandler implements IQueryHandler<DependenciesOfQuery, DependencyReport> {

    private final IssueLookup lookup;
    private final DependencyQueries dependencies;

    public DependenciesOfHandler(IssueLookup lookup, DependencyQueries dependencies) {
        this.lookup = lookup;
        this.dependencies = dependencies;
    }

    @Override
    public DependencyReport execute(DependenciesOfQuery query) {
        JiraIssue issue = lookup.getIssue(query.getIssueKey());
        String epicKey = issue.epicKey();
        List<TeamDependency> onEpic = (epicKey == null || epicKey.isBlank())
                ? List.of() : dependencies.teamDependencies(List.of(epicKey));
        return new DependencyReport(query.getIssueKey(), dependencies.teamName(), onEpic);
    }
}
