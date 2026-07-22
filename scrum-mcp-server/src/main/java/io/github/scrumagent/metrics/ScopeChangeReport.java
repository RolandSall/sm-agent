package io.github.scrumagent.metrics;

import java.util.List;

/**
 * Curated scope-change ingredients for the active sprint — the raw GROUP+SUM off the changelog,
 * with no verdict. {@code committedAtStart} is the original commitment (it includes the points of
 * issues later removed); {@code addedPoints}/{@code removedPoints} are the totals moved in/out after
 * start, and {@code added}/{@code removed} list the individual issues behind those totals. Comparing
 * added vs removed vs the commitment (grew / shrank / churned) is a reasoning step left to the
 * sprint-metrics skill, not collapsed into a signed net here.
 *
 * <p>Note: issues removed from a sprint are only visible if the Agile members endpoint still returns
 * them; production reporting of removals is best-effort until the sprint-report API is wired (phase 2),
 * so {@code removedPoints} is a lower bound.
 */
public record ScopeChangeReport(
        String sprint,
        double committedAtStart,
        double addedPoints,
        double removedPoints,
        List<ScopeChangeItem> added,
        List<ScopeChangeItem> removed) {

    public record ScopeChangeItem(String key, double storyPoints) {
    }
}
