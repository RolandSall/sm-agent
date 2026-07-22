package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.risk.RiskReport;
import io.github.scrumagent.risk.SprintRiskQuery;
import io.github.scrumagent.shared.ReadTool;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** MCP tool surface for the active-sprint delivery-risk ingredients. Read-only. */
@Component
public class RiskTools implements ReadTool {

    private final MediatorBus mediator;

    public RiskTools(MediatorBus mediator) {
        this.mediator = mediator;
    }

    @Tool(name = "sprint_delivery_risk", description = """
            [read-only] Raw delivery-risk ingredients for the active sprint: the stories (with their
            Epic + estimate), the cross-team Dependency issues on those Epics, and the team's remaining
            capacity. YOU do the reasoning (see the delivery-risk skill): join stories to dependencies
            by epicKey, keep the OPEN ones where our team is the waiting side, and judge each story's
            level. The tool computes no level. Provide capacity if known; omitting it flags
            capacityKnown=false rather than guessing.""")
    public RiskReport sprintDeliveryRisk(
            @ToolParam(required = false) Double capacityCommittedPoints,
            @ToolParam(required = false) Double capacityAvailablePoints,
            @ToolParam(required = false, description = "Only include stories of this type, e.g. Story (default: all types)") String issueType,
            @ToolParam(required = false, description = "Only include stories assigned to this person (default: all assignees)") String assignee) {
        return mediator.query(new SprintRiskQuery(capacityCommittedPoints, capacityAvailablePoints,
                issueType, assignee));
    }
}
