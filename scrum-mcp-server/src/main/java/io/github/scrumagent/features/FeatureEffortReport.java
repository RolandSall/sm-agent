package io.github.scrumagent.features;

import io.github.scrumagent.jira.FeatureEffort;

import java.util.List;

/**
 * Per-feature effort roll-up result — INGREDIENTS ONLY. The agent assesses which features are "off by
 * a lot" (the meaningful signal is Σ estimate vs Σ logged hours, and hours-per-point across features,
 * since dev-job points are a drift-prone reference). No verdict is computed here.
 *
 * @param features one entry per Epic in scope
 */
public record FeatureEffortReport(List<FeatureEffort> features) {
}
