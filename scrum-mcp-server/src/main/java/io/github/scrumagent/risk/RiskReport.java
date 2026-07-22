package io.github.scrumagent.risk;

import io.github.scrumagent.jira.TeamDependency;

import java.util.List;

/**
 * Raw delivery-risk ingredients for one sprint — no verdict. The agent joins {@code stories} to
 * {@code dependencies} by {@code epicKey}, keeps the OPEN ones where our team is the waiting side, and
 * reasons about each story's risk level (see the delivery-risk skill). The handler only fetches.
 *
 * @param sprint                 sprint label ({@code "active-sprint"})
 * @param stories                the active-sprint stories with their raw facts
 * @param dependencies           the cross-team Dependency issues on those stories' Epics (raw, unfiltered)
 * @param remainingCapacityPoints team headroom, or {@code null} when capacity is unknown
 * @param capacityKnown          whether capacity was resolved rather than guessed
 */
public record RiskReport(String sprint, List<StoryRisk> stories, List<TeamDependency> dependencies,
                         Double remainingCapacityPoints, boolean capacityKnown) {
}
