package io.github.scrumagent.dependencies;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: cross-team dependency radar for one issue — EVERY Dependency on that issue's Epic,
 * raw. No resolved-status filter: the "open, our-team-waiting" rule is applied by the agent from the
 * returned {@code ourTeam} and each row's status (see the cross-team-dependencies skill).
 */
public class DependenciesOfQuery implements IQuery<DependencyReport> {

    private final String issueKey;

    public DependenciesOfQuery(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getIssueKey() {
        return issueKey;
    }
}
