package io.github.scrumagent.features.internal;

import io.github.scrumagent.features.FeatureEffortQuery;
import io.github.scrumagent.features.FeatureEffortReport;
import io.github.scrumagent.jira.FeatureEffortQueries;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

/**
 * Maps the gateway's per-Epic effort roll-up into the report. All the arithmetic (live sums,
 * aggregate-vs-timetracking fallback, active-sprint scope derivation) lives in the gateway; this
 * handler is a thin pass-through that keeps the ingredients unassessed.
 */
@QueryHandler(FeatureEffortQuery.class)
public class FeatureEffortHandler implements IQueryHandler<FeatureEffortQuery, FeatureEffortReport> {

    private final FeatureEffortQueries jira;

    public FeatureEffortHandler(FeatureEffortQueries jira) {
        this.jira = jira;
    }

    @Override
    public FeatureEffortReport execute(FeatureEffortQuery query) {
        return new FeatureEffortReport(jira.featureEffort(query.getEpicKeys()));
    }
}
