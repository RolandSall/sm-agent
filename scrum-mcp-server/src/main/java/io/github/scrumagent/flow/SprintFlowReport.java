package io.github.scrumagent.flow;

import java.util.List;

/**
 * Curated daily-standup flow view of the active sprint: a single FLAT list of every active-sprint
 * issue with its raw, changelog-derived flow ingredients. No bucketing, no thresholds, no verdict —
 * the agent applies the stuck / stalled / reopened rules against the team's lines (see the
 * sprint-flow skill). The Java side only fetches, scopes by issue-type/assignee, and computes the
 * date-diff arithmetic; it never decides what is "stuck".
 *
 * @param issues every active-sprint issue (after any issue-type/assignee scoping), each carrying its
 *        raw flow ingredients
 */
public record SprintFlowReport(List<FlowIssue> issues) {

    /**
     * One issue's raw flow ingredients. {@code statusCategory} is Jira's built-in bucket ({@code To
     * Do} / {@code In Progress} / {@code Done}) passed through verbatim so the agent never re-derives
     * it. Day counts are boxed so "unknown" (no changelog signal) stays distinct from zero — an
     * unknown age is not "stuck", it is unknown.
     *
     * @param key issue key
     * @param summary issue summary
     * @param assignee display name, or {@code Unassigned}
     * @param statusCategory Jira status category — {@code To Do} / {@code In Progress} / {@code Done}
     * @param status the concrete workflow status name
     * @param ageInStatusDays whole days since the latest status change, or {@code null} if the issue
     *        has never changed status (so we cannot tell how long it has sat where it is)
     * @param daysSinceUpdated whole days since {@code fields.updated}, or {@code null} if absent
     * @param reopenCount status changes made after the issue first reached a done-like status — a raw
     *        churn proxy
     */
    public record FlowIssue(
            String key,
            String summary,
            String assignee,
            String statusCategory,
            String status,
            Long ageInStatusDays,
            Long daysSinceUpdated,
            int reopenCount) {
    }
}
