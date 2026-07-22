package io.github.scrumagent.jira.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.scrumagent.jira.JiraSprint;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.github.scrumagent.jira.internal.JiraHttp.orEmpty;
import static io.github.scrumagent.jira.internal.JiraHttp.requireBody;
import static io.github.scrumagent.jira.internal.JiraHttp.warnIfTruncated;
import static io.github.scrumagent.jira.internal.JiraTime.parseInstant;

/**
 * Resolves the board id and the current / listed sprints — the "which board, which sprint" seam.
 * A board can have several active sprints (PREP, parallel teams, PI overlaps), so
 * {@link #activeSprint()} narrows by the configured name substring then by today's date. Moved
 * verbatim from the former JiraClient god class so resolution behaviour is unchanged.
 */
@Component
class BoardLocator {

    /** Agile board sprint-list page size. */
    private static final int SPRINT_PAGE_SIZE = 100;

    private final JiraHttp http;
    private final JiraProperties props;
    private final SprintMapper sprintMapper;

    BoardLocator(JiraHttp http, JiraProperties props, SprintMapper sprintMapper) {
        this.http = http;
        this.props = props;
        this.sprintMapper = sprintMapper;
    }

    JiraSprint activeSprint() {
        List<JiraSprint> active = sprints("active");
        // A board can have several active sprints (PREP, parallel teams, PI overlaps). Narrow to the
        // team's by the configured name substring, then to the one whose date range contains today.
        String filter = props.sprintNameFilter();
        List<JiraSprint> candidates = active;
        if (filter != null && !filter.isBlank()) {
            List<JiraSprint> named = active.stream()
                    .filter(s -> s.name() != null && s.name().toLowerCase().contains(filter.toLowerCase()))
                    .toList();
            if (!named.isEmpty()) {
                candidates = named;
            }
        }
        Instant now = Instant.now();
        return candidates.stream()
                .filter(s -> s.startDate() != null && s.endDate() != null
                        && !now.isBefore(s.startDate()) && !now.isAfter(s.endDate()))
                .findFirst()
                .orElse(candidates.isEmpty() ? null : candidates.get(0));
    }

    List<JiraSprint> sprints(String state) {
        int boardId = resolveBoardId();
        JsonNode json = orEmpty(http.get(uri -> uri.path("/rest/agile/1.0/board/{boardId}/sprint")
                        .queryParam("state", state)
                        .queryParam("maxResults", SPRINT_PAGE_SIZE)
                        .build(boardId)));
        warnIfTruncated(json, "values", SPRINT_PAGE_SIZE, "board/" + boardId + "/sprint?state=" + state);
        List<JiraSprint> out = new ArrayList<>();
        for (JsonNode sprint : json.path("values")) {
            out.add(sprintMapper.toSprint(sprint));
        }
        return out;
    }

    int resolveBoardId() {
        if (props.boardId() != null) {
            return props.boardId();
        }
        String projectKey = props.projectKeys().get(0);
        JsonNode json = http.get(uri -> uri.path("/rest/agile/1.0/board")
                        .queryParam("projectKeyOrId", projectKey)
                        .build());
        JsonNode first = requireBody(json, "board?projectKeyOrId=" + projectKey).path("values").path(0);
        if (first.isMissingNode() || !first.hasNonNull("id")) {
            // Falling through to a 0 here would silently query /board/0/... — fail loudly instead.
            throw new IllegalStateException("No Agile board found for project " + projectKey
                    + "; set scrum.jira.board-id explicitly");
        }
        return first.path("id").asInt();
    }

    Instant sprintStart(long sprintId) {
        JsonNode json = http.get(uri -> uri.path("/rest/agile/1.0/sprint/{id}").build(sprintId));
        return parseInstant(requireBody(json, "sprint/" + sprintId).path("startDate").asText(null));
    }
}
