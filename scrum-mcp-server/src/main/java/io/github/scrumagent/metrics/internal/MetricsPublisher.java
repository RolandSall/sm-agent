package io.github.scrumagent.metrics.internal;

/**
 * Port for publishing a rendered metrics summary. Phase 1 logs it ({@link LoggingMetricsPublisher});
 * phase 2 supplies a Confluence-backed implementation — no change to the commit handler.
 */
public interface MetricsPublisher {

    void publish(String content);
}
