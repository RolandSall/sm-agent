package io.github.scrumagent.metrics.internal;

import io.github.scrumagent.metrics.MetricsPublicationPreview;
import io.github.scrumagent.metrics.PrepareMetricsPublicationQuery;
import io.github.scrumagent.metrics.ScopeChangeQuery;
import io.github.scrumagent.metrics.ScopeChangeReport;
import io.github.scrumagent.metrics.VelocityQuery;
import io.github.scrumagent.metrics.VelocityReport;
import io.github.scrumagent.shared.GovernanceToken;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.bus.MediatorBus;
import io.github.springmediator.mediator.core.IQueryHandler;

import java.time.Duration;
import java.time.Instant;

/**
 * Renders a sprint-metrics summary from the velocity and scope reports and registers it for later
 * commit. Writes nothing — it only reads and stages a preview behind a governance token. (Burndown
 * is intentionally omitted: Jira renders that chart itself.)
 *
 * <p>Sanctioned HITL exception: this is modeled as an {@code IQuery} even though registering the
 * preview mutates server-side governance state. It stays a query (not a command) because it publishes
 * nothing to Jira; the actual write is the separate token-gated {@code commit} command. Its REST
 * surface is a POST, not a GET, to reflect that state change.
 */
@QueryHandler(PrepareMetricsPublicationQuery.class)
public class PrepareMetricsPublicationHandler
        implements IQueryHandler<PrepareMetricsPublicationQuery, MetricsPublicationPreview> {

    private final MediatorBus mediator;
    private final PreviewRegistry previews;

    public PrepareMetricsPublicationHandler(MediatorBus mediator, PreviewRegistry previews) {
        this.mediator = mediator;
        this.previews = previews;
    }

    @Override
    public MetricsPublicationPreview execute(PrepareMetricsPublicationQuery query) {
        VelocityReport velocity = mediator.query(new VelocityQuery(null));
        ScopeChangeReport scope = mediator.query(new ScopeChangeQuery());

        String content = render(velocity, scope);
        GovernanceToken token = previews.register(content);
        return new MetricsPublicationPreview(
                token.value(),
                "(pending Confluence gateway)",
                "Sprint metrics summary",
                content,
                Instant.now().plus(Duration.ofMinutes(15)));
    }

    private String render(VelocityReport velocity, ScopeChangeReport scope) {
        StringBuilder sb = new StringBuilder("# Sprint metrics summary\n\n");
        sb.append("## Velocity\n")
                .append("Average completed: ").append(round(velocity.averageVelocity())).append("\n");
        velocity.sprints().forEach(s -> sb.append("- ").append(s.sprint())
                .append(": committed ").append(round(s.committed()))
                .append(", completed ").append(round(s.completed())).append("\n"));
        sb.append("\n## Scope change\n");
        if (scope.sprint() != null) {
            sb.append("Committed at start ").append(round(scope.committedAtStart()))
                    .append("; added ").append(round(scope.addedPoints()))
                    .append(", removed ").append(round(scope.removedPoints())).append("\n");
        }
        return sb.toString();
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
