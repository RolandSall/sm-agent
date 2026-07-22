package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.jira.GetIssueQuery;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.shared.ReadTool;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP tool surface for the jira module. These @Tool methods are what the Copilot/Claude
 * harness sees over MCP. They own nothing but presentation — every call dispatches a
 * mediator query, so phase 2's ChatClient tool-calling loop reuses the exact same queries.
 */
@Component
public class JiraTools implements ReadTool {

    private final MediatorBus mediator;

    public JiraTools(MediatorBus mediator) {
        this.mediator = mediator;
    }

    @Tool(name = "get_issue", description = """
            Fetch a single Jira issue with its summary, status, assignee, labels,
            rendered description, acceptance criteria and issue links. Use this to
            inspect one ticket's details or its dependencies.""")
    public JiraIssue getIssue(
            @ToolParam(description = "Jira issue key, e.g. PROJ-123") String issueKey) {
        return mediator.query(new GetIssueQuery(issueKey));
    }
}
