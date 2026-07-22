package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.metrics.CommitMetricsPublicationCommand;
import io.github.scrumagent.shared.WriteTool;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * The write half of the publish HITL. Registered with the MCP server ONLY when
 * {@code scrum.governance.read-only=false} — so in the default read-only profile this tool does not
 * exist. The command handler also re-checks the flag, gating REST/CLI callers identically.
 */
@Component
@ConditionalOnProperty(prefix = "scrum.governance", name = "read-only", havingValue = "false")
public class MetricsPublishTools implements WriteTool {

    private final MediatorBus mediator;

    public MetricsPublishTools(MediatorBus mediator) {
        this.mediator = mediator;
    }

    @Tool(name = "commit_publish_metrics", description = """
            Publish the sprint-metrics summary previously staged by prepare_publish_metrics. Pass the
            token from that preview. Writes to the configured target. Requires write mode.""")
    public String commitPublishMetrics(
            @ToolParam(description = "The token returned by prepare_publish_metrics") String token) {
        mediator.send(new CommitMetricsPublicationCommand(token));
        return "Published the sprint-metrics summary.";
    }
}
