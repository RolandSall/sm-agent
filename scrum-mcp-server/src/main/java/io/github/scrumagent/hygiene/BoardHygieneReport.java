package io.github.scrumagent.hygiene;

import java.util.List;

/**
 * Board-hygiene result: the per-field data-quality signals for every active-sprint issue, so the
 * caller can apply its own definition-of-ready rather than seeing only the pre-flagged ones. There
 * is deliberately NO OR-combined "issues with gaps" count — that would bake a definition-of-ready
 * into the tool. The three aggregates below are each a factual COUNT over ONE field; the caller
 * decides which fields its team requires and combines only those.
 *
 * @param totalIssues issues examined in the active sprint (equals {@code issues.size()})
 * @param missingEstimateCount how many examined issues have no story-points value
 * @param missingAcceptanceCriteriaCount how many examined issues have no acceptance criteria
 * @param missingAssigneeCount how many examined issues are unassigned
 * @param issues one entry per active-sprint issue (clean ones included), carrying the raw per-field
 *               booleans
 */
public record BoardHygieneReport(
        int totalIssues,
        int missingEstimateCount,
        int missingAcceptanceCriteriaCount,
        int missingAssigneeCount,
        List<HygieneIssue> issues) {

    /**
     * One active-sprint issue with its per-field readiness signals — the raw ingredients the caller
     * applies its team's definition-of-ready over.
     *
     * @param missingEstimate no story-points value
     * @param missingAcceptanceCriteria no acceptance criteria
     * @param missingAssignee unassigned
     */
    public record HygieneIssue(
            String key,
            String summary,
            String assignee,
            boolean missingEstimate,
            boolean missingAcceptanceCriteria,
            boolean missingAssignee) {
    }
}
