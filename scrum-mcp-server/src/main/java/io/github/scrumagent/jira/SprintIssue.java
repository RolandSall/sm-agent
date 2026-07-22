package io.github.scrumagent.jira;

import java.time.Instant;

/**
 * Curated per-sprint issue for metric computation. {@code statusCategory} is Jira's built-in
 * bucket ({@code To Do} / {@code In Progress} / {@code Done}) — instance-independent, so metrics
 * never need configured status names. The changelog-derived fields drive burndown and scope-change.
 *
 * @param doneDate when the issue entered a Done-category status, or {@code null} if not done
 * @param addedAfterSprintStart the issue joined this sprint after it started (scope creep)
 * @param removedFromSprint the issue was pulled out of this sprint (scope reduction)
 */
public record SprintIssue(
        String key,
        Double storyPoints,
        String status,
        String statusCategory,
        Instant doneDate,
        boolean addedAfterSprintStart,
        boolean removedFromSprint) {
}
