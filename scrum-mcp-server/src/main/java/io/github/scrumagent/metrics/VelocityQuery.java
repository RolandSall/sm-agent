package io.github.scrumagent.metrics;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: average completed points over the last N closed sprints.
 * {@code sprints} is agent-supplied (Tier 2); {@code null} defaults to 6.
 */
public class VelocityQuery implements IQuery<VelocityReport> {

    private final Integer sprints;

    public VelocityQuery(Integer sprints) {
        this.sprints = sprints;
    }

    public Integer getSprints() {
        return sprints;
    }
}
