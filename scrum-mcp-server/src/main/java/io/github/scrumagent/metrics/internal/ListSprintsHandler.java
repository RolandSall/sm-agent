package io.github.scrumagent.metrics.internal;

import io.github.scrumagent.jira.SprintQueries;
import io.github.scrumagent.metrics.ListSprintsQuery;
import io.github.scrumagent.metrics.SprintListReport;
import io.github.springmediator.mediator.annotations.QueryHandler;
import io.github.springmediator.mediator.core.IQueryHandler;

/**
 * Enumerates the board's sprints in the requested state so the agent can suggest one by full name,
 * given only the board id. The gateway already talks to {@code /rest/agile/1.0/board/{id}/sprint} —
 * this only picks the state and surfaces the result unchanged.
 */
@QueryHandler(ListSprintsQuery.class)
public class ListSprintsHandler implements IQueryHandler<ListSprintsQuery, SprintListReport> {

    /** Default: the sprints a Scrum Master is choosing between are the ones running now. */
    private static final String DEFAULT_STATE = "active";

    private final SprintQueries jira;

    public ListSprintsHandler(SprintQueries jira) {
        this.jira = jira;
    }

    @Override
    public SprintListReport execute(ListSprintsQuery query) {
        String state = query.getState() == null || query.getState().isBlank()
                ? DEFAULT_STATE : query.getState();
        return new SprintListReport(jira.sprints(state));
    }
}
