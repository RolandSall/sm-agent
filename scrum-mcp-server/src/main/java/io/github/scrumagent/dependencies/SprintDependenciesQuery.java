package io.github.scrumagent.dependencies;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: cross-team dependency radar across the active sprint's Epics — EVERY Dependency on
 * them, raw. No resolved-status filter: the "open, our-team-waiting" rule is applied by the agent from
 * the returned {@code ourTeam} and each row's status (see the cross-team-dependencies skill).
 */
public class SprintDependenciesQuery implements IQuery<DependencyReport> {
}
