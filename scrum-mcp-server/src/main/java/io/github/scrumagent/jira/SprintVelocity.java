package io.github.scrumagent.jira;

/**
 * One sprint's velocity as Jira itself reports it (from the greenhopper velocity chart) — we do NOT
 * recompute this, we surface Jira's own numbers so they match the board's Velocity report.
 *
 * @param committed estimated points at sprint start
 * @param completed points completed
 */
public record SprintVelocity(String sprint, double committed, double completed) {
}
