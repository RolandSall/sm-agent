package io.github.scrumagent.worklog;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Mediator query: report the logged-vs-estimate ingredients for every estimated active-sprint issue.
 * {@code minEstimateHours} is a fact-filter (issues below the floor are excluded — variance is
 * meaningless below it) and optional; a {@code null} means no floor. The OVERRUN / FINISHED_EARLY /
 * NO_LOGGING judgement is the caller's, not this handler's — no threshold factors here.
 */
public class EstimateVarianceQuery implements IQuery<EstimateVarianceReport> {

    private final Integer minEstimateHours;

    public EstimateVarianceQuery(Integer minEstimateHours) {
        this.minEstimateHours = minEstimateHours;
    }

    public Integer getMinEstimateHours() {
        return minEstimateHours;
    }
}
