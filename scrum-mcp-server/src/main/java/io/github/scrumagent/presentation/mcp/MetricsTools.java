package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.metrics.ListSprintsQuery;
import io.github.scrumagent.metrics.MetricsPublicationPreview;
import io.github.scrumagent.metrics.PrepareMetricsPublicationQuery;
import io.github.scrumagent.metrics.ScopeChangeQuery;
import io.github.scrumagent.metrics.ScopeChangeReport;
import io.github.scrumagent.metrics.SprintListReport;
import io.github.scrumagent.metrics.VelocityQuery;
import io.github.scrumagent.metrics.VelocityReport;
import io.github.scrumagent.shared.ReadTool;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** MCP tool surface for sprint metrics, including the read half of the publish HITL. Read-only. */
@Component
public class MetricsTools implements ReadTool {

    private final MediatorBus mediator;

    public MetricsTools(MediatorBus mediator) {
        this.mediator = mediator;
    }

    @Tool(name = "sprint_velocity", description = """
            Jira's own velocity per recent sprint (committed vs completed) plus the average completed.
            These are Jira's numbers, not a re-computation. For the burndown chart, use Jira directly.
            Read-only.""")
    public VelocityReport sprintVelocity(
            @ToolParam(required = false, description = "How many recent sprints to average (default 6)") Integer velocitySprints) {
        return mediator.query(new VelocityQuery(velocitySprints));
    }

    @Tool(name = "list_sprints", description = """
            [read-only] List the board's sprints (id, full name, state, dates) so you can suggest \
            which sprint to target — given just the board id. Default state=active.""")
    public SprintListReport listSprints(
            @ToolParam(required = false, description = "Sprint state filter: active, future, closed "
                    + "or a comma-combination (default active)") String state) {
        return mediator.query(new ListSprintsQuery(state));
    }

    @Tool(name = "sprint_scope_change", description = """
            Raw scope ingredients for the active sprint: points committed at start, points added and
            points removed since start, plus the issue lists behind each. Computes no verdict — YOU
            compare added vs removed vs the commitment (grew / shrank / churned). Read-only.""")
    public ScopeChangeReport sprintScopeChange() {
        return mediator.query(new ScopeChangeQuery());
    }

    @Tool(name = "prepare_publish_metrics", description = """
            Render the sprint-metrics summary and return a preview plus a confirmation token WITHOUT
            publishing. Review the preview, then call commit_publish_metrics with the token to publish.
            Read-only (writes nothing).""")
    public MetricsPublicationPreview preparePublishMetrics() {
        return mediator.query(new PrepareMetricsPublicationQuery());
    }
}
