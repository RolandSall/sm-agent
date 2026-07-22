package io.github.scrumagent.metrics;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: enumerate the configured board's sprints so the agent can suggest which one to
 * target, given only the board id. {@code state} is agent-supplied (Tier 2) and optional — Jira
 * accepts {@code active}, {@code future}, {@code closed} or comma-combinations; a {@code null}/blank
 * value falls back to the handler's default of {@code active}.
 */
public class ListSprintsQuery implements IQuery<SprintListReport> {

    private final String state;

    public ListSprintsQuery(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
