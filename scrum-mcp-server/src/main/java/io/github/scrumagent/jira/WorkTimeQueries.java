package io.github.scrumagent.jira;

import java.util.List;

/** Time-tracking rollups for the active sprint. */
public interface WorkTimeQueries {

    /** Curated time-tracking rollup for every active-sprint issue (logged vs estimate). */
    List<JiraWorkTime> activeSprintWorkTime();
}
