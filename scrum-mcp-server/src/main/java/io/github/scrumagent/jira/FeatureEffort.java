package io.github.scrumagent.jira;

import java.util.List;

/**
 * Curated per-feature (Epic) effort roll-up — INGREDIENTS ONLY. No verdict is baked in: whether a
 * feature is "off by a lot" is the agent's judgement over these numbers.
 *
 * <p>Sums are computed LIVE from the child stories. The Epic's own {@code devJobPoints} snapshot is
 * surfaced only as a reference, because it drifts from the live child sum (see {@link StoryEffort}).
 *
 * @param epicKey the Epic ("feature") key
 * @param epicSummary the Epic's summary
 * @param devJobPoints the Epic's own "Planned Dev Job" story-points snapshot, or {@code null} — a
 *        reference value only; compare the live sums, not this
 * @param storyCount number of child stories rolled up
 * @param sumStoryPoints Σ child story points (nulls treated as 0)
 * @param sumEstimateHours Σ child original-estimate hours (nulls treated as 0)
 * @param sumLoggedHours Σ child logged hours (nulls treated as 0)
 * @param sumEstimateDays {@code sumEstimateHours} re-expressed as derived working-days (hours untouched)
 * @param sumLoggedDays {@code sumLoggedHours} re-expressed as derived working-days (hours untouched)
 * @param stories the child stories that make up the sums
 */
public record FeatureEffort(
        String epicKey,
        String epicSummary,
        Double devJobPoints,
        int storyCount,
        double sumStoryPoints,
        double sumEstimateHours,
        double sumLoggedHours,
        double sumEstimateDays,
        double sumLoggedDays,
        List<StoryEffort> stories) {
}
