package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.shared.ReadTool;
import io.github.scrumagent.worklog.EstimateVarianceQuery;
import io.github.scrumagent.worklog.EstimateVarianceReport;
import io.github.scrumagent.worklog.WorkloadQuery;
import io.github.scrumagent.worklog.WorkloadReport;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** MCP tool surface for logged-work rollups. Read-only. */
@Component
public class WorklogTools implements ReadTool {

    private final MediatorBus mediator;

    public WorklogTools(MediatorBus mediator) {
        this.mediator = mediator;
    }

    @Tool(name = "worklog_summary", description = """
            Per-assignee logged hours vs original estimate vs remaining (with team totals) for the
            active sprint, from the configured logged-hours field. Use to spot under/over-logging.
            Read-only.""")
    public WorkloadReport worklogSummary(
            @ToolParam(required = false, description = "Only roll up issues of this type, e.g. Story (default: all types)") String issueType,
            @ToolParam(required = false, description = "Only roll up issues assigned to this person (default: all assignees)") String assignee,
            @ToolParam(required = false, description = "Working hours per day used to derive the *Days figures (default: the configured working-hours-per-day)") Double hoursPerDay) {
        return mediator.query(new WorkloadQuery(issueType, assignee, hoursPerDay));
    }

    @Tool(name = "estimate_variance", description = "[read-only] Reports logged time vs the original "
            + "estimate for every estimated active-sprint issue — raw ingredients only: usageRatio "
            + "(logged/estimate), statusCategory, logged/estimate hours and days. Uses native "
            + "worklog/timetracking. Computes no verdict; apply your team's line (see the "
            + "estimate-variance skill).")
    public EstimateVarianceReport estimateVariance(
            @ToolParam(required = false, description = "Only consider issues whose original estimate is at least this many hours") Integer minEstimateHours) {
        return mediator.query(new EstimateVarianceQuery(minEstimateHours));
    }
}
