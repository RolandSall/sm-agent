package io.github.scrumagent.jira;

import java.util.List;

/** Changelog-derived flow signals for the active sprint. */
public interface SprintFlowQueries {

    /** Active-sprint issues with changelog-derived flow signals (age-in-status, staleness, reopens). */
    List<SprintIssueFlow> activeSprintFlow();
}
