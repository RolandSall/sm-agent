package io.github.scrumagent.jira;

/**
 * One child story's effort ingredients within a {@link FeatureEffort}.
 *
 * <p>Hours are seconds/3600. Estimate and logged hours prefer Jira's aggregate fields
 * ({@code aggregatetimeoriginalestimate} / {@code aggregatetimespent}) so sub-task time is included,
 * falling back to the issue's own {@code timetracking} when the aggregate is absent. Any of these may
 * be {@code null} (no estimate, nobody logged) — the roll-up treats null as 0 when summing.
 *
 * @param key the story key
 * @param status the story's status name
 * @param points story points, or {@code null} if unestimated
 * @param estimateHours original estimate in hours, or {@code null}
 * @param loggedHours logged work in hours, or {@code null}
 * @param estimateDays {@code estimateHours} re-expressed as derived working-days (0 when hours null)
 * @param loggedDays {@code loggedHours} re-expressed as derived working-days (0 when hours null)
 */
public record StoryEffort(
        String key,
        String status,
        Double points,
        Double estimateHours,
        Double loggedHours,
        double estimateDays,
        double loggedDays) {
}
