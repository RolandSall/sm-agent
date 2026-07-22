package io.github.scrumagent.metrics;

import io.github.scrumagent.jira.JiraSprint;

import java.util.List;

/**
 * Curated list of the board's sprints (id, full name, state, dates and goal) — the raw enumeration a
 * human uses to pick which sprint to target. These are Jira's own sprint records surfaced unchanged;
 * nothing is computed here.
 */
public record SprintListReport(List<JiraSprint> sprints) {
}
