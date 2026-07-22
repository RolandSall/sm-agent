/**
 * Sprint-metrics capability: velocity (surfaced from Jira's own velocity chart, not recomputed) and
 * scope-change (from the sprint changelog), plus the prepare→commit human-in-the-loop publication
 * exemplar. Burndown is intentionally NOT here — Jira renders that chart itself, and a hand-rolled
 * copy would disagree with it. Reads the jira gateway; reads are safe, commit is gated by
 * {@code scrum.governance.read-only}.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"jira", "shared"})
package io.github.scrumagent.metrics;
