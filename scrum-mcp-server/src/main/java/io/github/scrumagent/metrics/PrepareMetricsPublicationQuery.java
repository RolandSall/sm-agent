package io.github.scrumagent.metrics;

import io.github.springmediator.mediator.core.IQuery;

/**
 * Read side of the human-in-the-loop publish: renders the sprint-metrics summary and returns a
 * preview + token WITHOUT writing anything. A human reviews it, then dispatches the commit.
 */
public class PrepareMetricsPublicationQuery implements IQuery<MetricsPublicationPreview> {
}
