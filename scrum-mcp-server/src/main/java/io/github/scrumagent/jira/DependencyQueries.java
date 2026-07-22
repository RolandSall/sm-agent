package io.github.scrumagent.jira;

import java.util.List;

/** Cross-team dependency reads over the {@code Dependency} issue type linked to Epics. */
public interface DependencyQueries {

    /**
     * The {@code Dependency}-type issues linked (via Epic Link) to any of the given epics. Returns an
     * empty list when {@code epicKeys} is {@code null} or empty (no epics, nothing to look up).
     */
    List<TeamDependency> teamDependencies(List<String> epicKeys);

    /**
     * Our team's identity value as it appears in the Dependency {@code Depend On}/{@code Dependent/s}
     * option fields — the anchor that separates "a team we wait on" from "a team waiting on us".
     */
    String teamName();
}
