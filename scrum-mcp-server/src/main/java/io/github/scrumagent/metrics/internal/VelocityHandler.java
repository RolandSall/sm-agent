package io.github.scrumagent.metrics.internal;

import io.github.scrumagent.jira.SprintQueries;
import io.github.scrumagent.jira.SprintVelocity;
import io.github.scrumagent.metrics.VelocityQuery;
import io.github.scrumagent.metrics.VelocityReport;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.util.List;

/**
 * Surfaces Jira's own velocity numbers and averages the completed column over the most recent
 * sprints. We deliberately do NOT recompute per-sprint velocity — Jira already computes it, and a
 * hand-rolled version would disagree with the board's Velocity report.
 */
@QueryHandler(VelocityQuery.class)
public class VelocityHandler implements IQueryHandler<VelocityQuery, VelocityReport> {

    private static final int DEFAULT_SPRINTS = 6;

    private final SprintQueries jira;

    public VelocityHandler(SprintQueries jira) {
        this.jira = jira;
    }

    @Override
    public VelocityReport execute(VelocityQuery query) {
        int limit = query.getSprints() == null ? DEFAULT_SPRINTS : query.getSprints();

        List<SprintVelocity> all = jira.velocity();
        List<SprintVelocity> recent = all.size() > limit ? all.subList(all.size() - limit, all.size()) : all;

        double avg = recent.isEmpty() ? 0
                : recent.stream().mapToDouble(SprintVelocity::completed).average().orElse(0);
        return new VelocityReport(recent, avg);
    }
}
