package io.github.scrumagent.features;

import io.github.springmediator.mediator.core.IQuery;

import java.util.List;

/**
 * Mediator query: roll up per-feature (Epic) effort ingredients. The Epic keys are agent-supplied
 * (Tier 2) and optional — a {@code null}/empty list means "derive the scope from the active sprint's
 * epics" (the handler/gateway resolves it).
 */
public class FeatureEffortQuery implements IQuery<FeatureEffortReport> {

    private final List<String> epicKeys;

    public FeatureEffortQuery(List<String> epicKeys) {
        this.epicKeys = epicKeys;
    }

    public List<String> getEpicKeys() {
        return epicKeys;
    }
}
