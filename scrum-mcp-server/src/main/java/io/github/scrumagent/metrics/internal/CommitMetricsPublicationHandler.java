package io.github.scrumagent.metrics.internal;

import io.github.scrumagent.metrics.CommitMetricsPublicationCommand;
import io.github.scrumagent.shared.GovernanceProperties;
import io.github.springmediator.mediator.annotations.CommandHandler;
import io.github.springmediator.mediator.core.ICommandHandler;

import java.util.Optional;

/**
 * Publishes a previously prepared summary. Belt-and-braces governance: even though the write tool
 * is not registered in read-only mode, this handler also refuses to act when read-only — so a
 * REST/CLI caller is gated identically. The token must match a live preview.
 */
@CommandHandler(CommitMetricsPublicationCommand.class)
public class CommitMetricsPublicationHandler implements ICommandHandler<CommitMetricsPublicationCommand> {

    private final PreviewRegistry previews;
    private final GovernanceProperties governance;
    private final MetricsPublisher publisher;

    public CommitMetricsPublicationHandler(PreviewRegistry previews,
                                           GovernanceProperties governance,
                                           MetricsPublisher publisher) {
        this.previews = previews;
        this.governance = governance;
        this.publisher = publisher;
    }

    @Override
    public void execute(CommitMetricsPublicationCommand command) {
        if (governance.readOnly()) {
            throw new IllegalStateException(
                    "Refusing to publish: server is in read-only mode (scrum.governance.read-only=true).");
        }
        Optional<String> content = previews.consume(command.getToken());
        if (content.isEmpty()) {
            throw new IllegalArgumentException(
                    "No pending preview for that token — call prepare_publish_metrics first (or it expired).");
        }
        publisher.publish(content.get());
    }
}
