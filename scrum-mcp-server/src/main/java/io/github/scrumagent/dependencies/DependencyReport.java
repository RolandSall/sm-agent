package io.github.scrumagent.dependencies;

import io.github.scrumagent.jira.TeamDependency;

import java.util.List;

/**
 * Raw cross-team dependency ingredients — no verdict, no filter. Returns EVERY {@code Dependency}
 * issue on the scope's Epics (open and resolved, both directions), plus {@code ourTeam} so the agent
 * can apply the direction rule itself: keep the OPEN ones where {@code waitingTeam == ourTeam} and
 * {@code blockingTeam != ourTeam} (see the cross-team-dependencies skill). The handler only fetches.
 *
 * @param rootIssue the issue the radar was run for, or {@code "active-sprint"} for the whole-sprint
 *                  variant
 * @param ourTeam the configured team identity ({@code Depend On}/{@code Dependent/s} option value)
 *                the agent compares against to decide direction
 * @param dependencies every Dependency on the scope's Epics, RAW — resolved and reverse-direction
 *                     rows are NOT dropped; the agent filters
 */
public record DependencyReport(String rootIssue, String ourTeam, List<TeamDependency> dependencies) {
}
