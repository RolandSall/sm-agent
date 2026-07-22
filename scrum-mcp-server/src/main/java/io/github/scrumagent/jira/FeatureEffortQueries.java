package io.github.scrumagent.jira;

import java.util.List;

/** Per-feature (Epic) effort roll-ups. */
public interface FeatureEffortQueries {

    /**
     * Per-feature (Epic) effort roll-up: for each Epic, its child stories summed into live effort
     * ingredients (Σ points / Σ estimate / Σ logged hours) plus the Epic's own dev-job snapshot. When
     * {@code epicKeys} is null/empty the scope is derived from the DISTINCT Epic-Links of the active
     * sprint's issues.
     */
    List<FeatureEffort> featureEffort(List<String> epicKeys);
}
