package io.github.scrumagent.jira;

import java.util.List;

/** Sprint-level lookups: the sprint list, the active sprint, its issues and velocity. */
public interface SprintQueries {

    /** Sprints on the configured board in the given state ({@code active}/{@code closed}/{@code future}). */
    List<JiraSprint> sprints(String state);

    /**
     * THE team's current sprint — a board can have several sprints active at once, so this picks the
     * one matching the configured name filter and today's date. {@code null} if none is active.
     */
    JiraSprint activeSprint();

    /** Jira's own velocity numbers per recent sprint (greenhopper velocity chart) — not recomputed. */
    List<SprintVelocity> velocity();

    /** Curated issues of one sprint, with changelog-derived done-date and scope-change flags. */
    List<SprintIssue> sprintIssues(long sprintId);
}
