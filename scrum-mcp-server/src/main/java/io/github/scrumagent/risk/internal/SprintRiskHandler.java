package io.github.scrumagent.risk.internal;

import io.github.scrumagent.capacity.CapacityGateway;
import io.github.scrumagent.jira.DependencyQueries;
import io.github.scrumagent.jira.IssueSearch;
import io.github.scrumagent.jira.TeamDependency;
import io.github.scrumagent.risk.RiskReport;
import io.github.scrumagent.risk.SprintRiskQuery;
import io.github.scrumagent.risk.StoryRisk;
import io.github.scrumagent.shared.IssueFilters;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

/**
 * Delivery-risk is a FETCH, not a verdict. It returns the active sprint's stories, the cross-team
 * Dependency issues on their Epics, and the team's capacity headroom — all raw. The agent joins
 * stories to dependencies by Epic, keeps the open ones where our team is the waiting side, and reasons
 * about the risk level (see the delivery-risk skill). The only logic here is the fetch, the optional
 * issue filter, and picking a capacity source.
 */
@QueryHandler(SprintRiskQuery.class)
public class SprintRiskHandler implements IQueryHandler<SprintRiskQuery, RiskReport> {

    private final IssueSearch issueSearch;
    private final DependencyQueries dependencies;
    private final ObjectProvider<CapacityGateway> capacityProvider;

    public SprintRiskHandler(IssueSearch issueSearch, DependencyQueries dependencies,
                             ObjectProvider<CapacityGateway> capacityProvider) {
        this.issueSearch = issueSearch;
        this.dependencies = dependencies;
        this.capacityProvider = capacityProvider;
    }

    @Override
    public RiskReport execute(SprintRiskQuery query) {
        List<StoryRisk> stories = issueSearch.activeSprintIssues().stream()
                .filter(i -> IssueFilters.matches(query.getIssueType(), i.type())
                        && IssueFilters.matches(query.getAssignee(), i.assignee()))
                .map(i -> new StoryRisk(i.key(), i.summary(), i.epicKey(), i.storyPoints(),
                        i.storyPoints() == null))
                .toList();

        List<String> epicKeys = stories.stream()
                .map(StoryRisk::epicKey)
                .filter(k -> k != null && !k.isBlank())
                .distinct()
                .toList();
        List<TeamDependency> deps = dependencies.teamDependencies(epicKeys);

        Double remaining;
        boolean capacityKnown;
        CapacityGateway cg = capacityProvider.getIfAvailable();
        if (cg != null) {
            remaining = cg.getTeamCapacity(null).remainingPoints();
            capacityKnown = true;
        } else if (query.getCapacityCommittedPoints() != null
                && query.getCapacityAvailablePoints() != null) {
            remaining = query.getCapacityAvailablePoints() - query.getCapacityCommittedPoints();
            capacityKnown = true;
        } else {
            remaining = null;
            capacityKnown = false;
        }

        return new RiskReport("active-sprint", stories, deps, remaining, capacityKnown);
    }
}
