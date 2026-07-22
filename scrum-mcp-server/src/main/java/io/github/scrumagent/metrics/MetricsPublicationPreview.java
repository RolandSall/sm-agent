package io.github.scrumagent.metrics;

import java.time.Instant;

/**
 * The proposed publication a human reviews before committing. {@code token} is passed to
 * {@code commit_publish_metrics} to publish exactly this content.
 */
public record MetricsPublicationPreview(
        String token,
        String targetSpace,
        String targetTitle,
        String renderedContent,
        Instant expiresAt) {
}
