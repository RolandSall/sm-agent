package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.hygiene.BoardHygieneQuery;
import io.github.scrumagent.hygiene.BoardHygieneReport;
import io.github.scrumagent.shared.ReadTool;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** MCP tool surface for board hygiene. Read-only. */
@Component
public class HygieneTools implements ReadTool {

    private final MediatorBus mediator;

    public HygieneTools(MediatorBus mediator) {
        this.mediator = mediator;
    }

    @Tool(name = "board_hygiene", description = """
            Flag active-sprint issues that are missing a story-point estimate, acceptance criteria,
            or an assignee. Use before standup / sprint planning to check the board is ready.
            Read-only.""")
    public BoardHygieneReport boardHygiene(
            @ToolParam(required = false, description = "Only examine issues of this type, e.g. Story (default: all types)") String issueType,
            @ToolParam(required = false, description = "Only examine issues assigned to this person (default: all assignees)") String assignee) {
        return mediator.query(new BoardHygieneQuery(issueType, assignee));
    }
}
