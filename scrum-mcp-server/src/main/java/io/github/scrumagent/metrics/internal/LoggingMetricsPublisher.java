package io.github.scrumagent.metrics.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Interim publisher: logs the approved content. Replaced by a Confluence gateway in phase 2. */
@Component
class LoggingMetricsPublisher implements MetricsPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingMetricsPublisher.class);

    @Override
    public void publish(String content) {
        log.info("Publishing metrics summary:\n{}", content);
    }
}
