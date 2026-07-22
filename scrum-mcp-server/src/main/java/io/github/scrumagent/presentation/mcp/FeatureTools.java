package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.features.FeatureEffortQuery;
import io.github.scrumagent.features.FeatureEffortReport;
import io.github.scrumagent.shared.ReadTool;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/** MCP tool surface for the per-feature (Epic) effort roll-up. Read-only. */
@Component
public class FeatureTools implements ReadTool {

    private final MediatorBus mediator;

    public FeatureTools(MediatorBus mediator) {
        this.mediator = mediator;
    }

    @Tool(name = "feature_effort_rollup", description = """
            [read-only] Per-feature (Epic) effort INGREDIENTS: for each Epic, its child stories summed
            live into {devJobPoints (reference), sumStoryPoints, sumEstimateHours, sumLoggedHours,
            storyCount, stories[]}. Returns numbers only — NO verdict; YOU assess which features are
            "off by a lot" (the meaningful signal is HOURS: Σ estimate vs Σ logged, and hours-per-point
            across features). Omit epicKeys to scope from the active sprint's epics. Read-only.""")
    public FeatureEffortReport featureEffortRollup(
            @ToolParam(required = false, description = "Epic keys to roll up; omit to derive from the active sprint's epics") List<String> epicKeys) {
        return mediator.query(new FeatureEffortQuery(epicKeys));
    }
}
