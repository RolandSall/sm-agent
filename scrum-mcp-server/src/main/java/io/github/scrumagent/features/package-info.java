/**
 * Feature (Epic) effort-rollup capability: per-Epic effort INGREDIENTS — Σ story points, Σ estimate
 * hours, Σ logged hours, story count, and the Epic's own dev-job snapshot — computed live from the
 * child stories. No verdict is baked in: whether a feature is "off by a lot" is the agent's call over
 * the numbers (the meaningful signal is HOURS: Σ estimate vs Σ logged). "Feature = Epic" is a config
 * default ({@code scrum.jira.feature-issue-type}), swappable to a real "Feature" type. Reads the jira
 * gateway. Read-only.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"jira", "shared"})
package io.github.scrumagent.features;
