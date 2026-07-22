package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.flow.ReleaseQueueQuery;
import io.github.scrumagent.flow.ReleaseQueueReport;
import io.github.scrumagent.flow.SprintFlowQuery;
import io.github.scrumagent.flow.SprintFlowReport;
import io.github.scrumagent.flow.StoriesToTestQuery;
import io.github.scrumagent.flow.StoriesToTestReport;
import io.github.scrumagent.flow.WipReport;
import io.github.scrumagent.flow.WipStatusQuery;
import io.github.scrumagent.shared.ReadTool;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/** MCP tool surface for flow/queue state (WIP + release queue). Read-only. */
@Component
public class FlowTools implements ReadTool {

    private final MediatorBus mediator;

    public FlowTools(MediatorBus mediator) {
        this.mediator = mediator;
    }

    @Tool(name = "wip_status", description = """
            Count how many stories are in progress at once, per assignee and for the team. Returns
            exact counts and echoes the supplied limits as context; computes no over-limit verdict —
            the caller judges counts against limits. In-progress is detected via Jira's status
            category. Read-only.""")
    public WipReport wipStatus(
            @ToolParam(required = false, description = "Max in-progress per assignee before flagging") Integer wipLimitPerAssignee,
            @ToolParam(required = false, description = "Max in-progress across the team before flagging") Integer wipLimitTeam,
            @ToolParam(required = false, description = "Only count issues of this type, e.g. Story (default: all types)") String issueType,
            @ToolParam(required = false, description = "Only count issues assigned to this person (default: all assignees)") String assignee) {
        return mediator.query(new WipStatusQuery(wipLimitPerAssignee, wipLimitTeam, issueType, assignee));
    }

    @Tool(name = "release_queue", description = """
            Count issues sitting in the team's release / ready-to-test statuses (so the PM knows the
            test backlog), bucketed by status. Supply the team's status names. Read-only.""")
    public ReleaseQueueReport releaseQueue(
            @ToolParam(description = "The team's ready-to-test / release status names") List<String> releaseStatuses,
            @ToolParam(required = false, description = "Only count issues of this type, e.g. Story (default: all types)") String issueType,
            @ToolParam(required = false, description = "Only count issues assigned to this person (default: all assignees)") String assignee) {
        return mediator.query(new ReleaseQueueQuery(releaseStatuses, issueType, assignee));
    }

    @Tool(name = "stories_to_test", description = """
            [read-only] PO-facing: the dev-complete stories waiting for acceptance (across the
            configured projects, not sprint-limited), each with its acceptance criteria and parent
            epic. Omit statuses to use the default "Ready for Acceptance"; the exact "to test" status
            names are the team's call. Read-only.""")
    public StoriesToTestReport storiesToTest(
            @ToolParam(required = false, description = "The team's ready-to-test status names (default [\"Ready for Acceptance\"])") List<String> statuses,
            @ToolParam(required = false, description = "Only include issues of this type, e.g. Story (default: all types)") String issueType,
            @ToolParam(required = false, description = "Only include issues assigned to this person (default: all assignees)") String assignee) {
        return mediator.query(new StoriesToTestQuery(statuses, issueType, assignee));
    }

    @Tool(name = "sprint_flow", description = "[read-only] Daily-standup flow fetch: every "
            + "active-sprint ticket with its raw changelog-derived flow ingredients — statusCategory, "
            + "ageInStatusDays, daysSinceUpdated, reopenCount — as one flat list. Returns no buckets or "
            + "verdict; YOU apply the stuck / stalled / reopened lines (see the sprint-flow skill).")
    public SprintFlowReport sprintFlow(
            @ToolParam(required = false, description = "Only include issues of this type, e.g. Story (default: all types)") String issueType,
            @ToolParam(required = false, description = "Only include issues assigned to this person (default: all assignees)") String assignee) {
        return mediator.query(new SprintFlowQuery(issueType, assignee));
    }
}
