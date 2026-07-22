package io.github.scrumagent.flow;

import java.util.List;

/**
 * Curated WIP report — raw ingredients only. {@code teamInProgress} and each {@code inProgress} are
 * exact counts computed in code; {@code teamLimit}/{@code limit} are echoed from the supplied
 * parameters (Tier-2 policy) and may be {@code null}. No over-limit verdict is computed here — the
 * agent compares counts to limits (see the wip-limits skill).
 */
public record WipReport(
        int teamInProgress,
        Integer teamLimit,
        List<AssigneeWip> perAssignee) {

    public record AssigneeWip(String assignee, int inProgress, Integer limit) {
    }
}
