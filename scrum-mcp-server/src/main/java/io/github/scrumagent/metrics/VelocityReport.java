package io.github.scrumagent.metrics;

import io.github.scrumagent.jira.SprintVelocity;

import java.util.List;

/**
 * Curated velocity report. The per-sprint numbers are Jira's own (from its velocity chart); the only
 * thing computed here is the average of the completed column over the selected sprints.
 */
public record VelocityReport(List<SprintVelocity> sprints, double averageVelocity) {
}
