package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.dependencies.DependenciesOfQuery;
import io.github.scrumagent.dependencies.DependencyReport;
import io.github.scrumagent.dependencies.SprintDependenciesQuery;
import io.github.scrumagent.shared.ReadTool;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** MCP tool surface for the cross-team dependency radar. Read-only. */
@Component
public class DependencyTools implements ReadTool {

    private final MediatorBus mediator;

    public DependencyTools(MediatorBus mediator) {
        this.mediator = mediator;
    }

    @Tool(name = "dependency_radar", description = """
            [read-only] Cross-team dependencies of one issue: EVERY Dependency issue on the issue's
            Epic, raw and unfiltered (open AND resolved, both directions). Returns each Dependency's
            status, statusCategory, blockingTeam (Depend On), waitingTeam (Dependent/s), Epic and
            delivery sprint, plus ourTeam. Ingredients only — YOU keep the open ones where
            waitingTeam == ourTeam and blockingTeam != ourTeam (see the cross-team-dependencies skill).""")
    public DependencyReport dependencyRadar(
            @ToolParam(description = "Jira issue key, e.g. PROJ-123") String issueKey) {
        return mediator.query(new DependenciesOfQuery(issueKey));
    }

    @Tool(name = "sprint_dependency_radar", description = """
            [read-only] Cross-team dependency radar across the whole active sprint: EVERY Dependency
            issue on the sprint's Epics, raw and unfiltered (open AND resolved, both directions), plus
            ourTeam. Ingredients only — YOU apply the direction/open rule (see the
            cross-team-dependencies skill).""")
    public DependencyReport sprintDependencyRadar() {
        return mediator.query(new SprintDependenciesQuery());
    }
}
