package io.github.scrumagent.acceptance;

import org.mockserver.model.MediaType;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Small facade over the MockServer expectation API that registers Jira REST stubs matched exactly
 * the way the {@code JiraClient} issues them (path + the discriminating query parameters). Query
 * values are regexes (full-match), so the two search / two expand variants stay distinct. Keeps the
 * step definitions focused on scenario data rather than URL matching.
 *
 * <p>Public because the layered step definitions live in sub-packages of {@code acceptance} and
 * stub Jira from their {@code Given}/{@code Background} steps.
 */
public final class JiraStubs {

    private JiraStubs() {
    }

    /**
     * The active-sprint tools now resolve THE current sprint from the board first (a board can have
     * many active sprints), then query {@code sprint = <id>}. This stubs a single wide-date "RIO"
     * active sprint (id 100) so resolution picks it, then serves {@code body} for that sprint's issues.
     */
    public static void activeSprintSearch(String body) {
        activeSprints(1, JiraJson.sprintListResponse(JiraJson.sprint(100, "RIO Test Sprint", "active",
                "2020-01-01T00:00:00.000Z", "2099-01-01T00:00:00.000Z", null)));
        // Match the resolved sprint id exactly (not any "sprint = ...") so a wrong-sprint resolution
        // fails to match this stub and is detectable rather than silently served.
        get("/rest/api/2/search", "jql", ".*sprint = 100.*", body);
    }

    /**
     * The no-active-sprint world: the board resolves to zero active sprints, so {@code activeSprint()}
     * returns null and the active-sprint read tools short-circuit to an empty report without a search.
     */
    public static void emptyActiveSprint() {
        activeSprints(1, JiraJson.sprintListResponse());
    }

    /** GET /rest/api/2/search where the jql filters by "status in (...)" (release queue). */
    public static void releaseQueueSearch(String body) {
        get("/rest/api/2/search", "jql", ".*status in.*", body);
    }

    /**
     * GET /rest/api/2/search where the jql filters by "status in (...)" — the PO "stories to test"
     * search. Same JQL shape as the release queue; a distinct name keeps the step intent readable.
     */
    public static void storiesToTestSearch(String body) {
        get("/rest/api/2/search", "jql", ".*status in.*", body);
    }

    /** GET /rest/api/2/issue/{key}?fields=summary,... — the epic fetch for the feature roll-up. */
    public static void epicIssue(String key, String body) {
        get("/rest/api/2/issue/" + key, "fields", ".*", body);
    }

    /** GET /rest/api/2/search where the jql is {@code "Epic Link" = <epicKey>} (an epic's children). */
    public static void epicChildrenSearch(String epicKey, String body) {
        get("/rest/api/2/search", "jql", ".*Epic Link.*" + epicKey + ".*", body);
    }

    /**
     * GET /rest/api/2/search where the jql filters {@code issuetype = "Dependency" AND "Epic Link" in
     * (...)} — the cross-team dependency lookup. Distinct from the active-sprint and epic-children
     * searches by the {@code issuetype = ...Dependency} clause.
     */
    public static void dependencySearch(String body) {
        get("/rest/api/2/search", "jql", ".*issuetype = .*Dependency.*", body);
    }

    /** GET /rest/agile/1.0/board/{boardId}/sprint?state=closed. */
    public static void closedSprints(int boardId, String body) {
        get("/rest/agile/1.0/board/" + boardId + "/sprint", "state", "closed", body);
    }

    /** GET /rest/agile/1.0/board/{boardId}/sprint?state=active. */
    public static void activeSprints(int boardId, String body) {
        get("/rest/agile/1.0/board/" + boardId + "/sprint", "state", "active", body);
    }

    /** GET /rest/greenhopper/1.0/rapid/charts/velocity?rapidViewId={boardId} (Jira's velocity chart). */
    public static void velocity(int boardId, String body) {
        get("/rest/greenhopper/1.0/rapid/charts/velocity", "rapidViewId", String.valueOf(boardId), body);
    }

    /** GET /rest/agile/1.0/sprint/{id} (used to read a sprint's startDate). */
    public static void sprintStart(long sprintId, String body) {
        get("/rest/agile/1.0/sprint/" + sprintId, body);
    }

    /** GET /rest/agile/1.0/sprint/{id}/issue?expand=changelog. */
    public static void sprintIssues(long sprintId, String body) {
        get("/rest/agile/1.0/sprint/" + sprintId + "/issue", body);
    }

    /** GET /rest/api/2/issue/{key}?expand=renderedFields (single issue, no changelog). */
    public static void issue(String key, String body) {
        get("/rest/api/2/issue/" + key, "expand", "renderedFields", body);
    }

    // --- helpers ---

    private static void get(String path, String param, String valueRegex, String body) {
        MockServerSupport.server()
                .when(request().withMethod("GET").withPath(path)
                        .withQueryStringParameter(param, valueRegex))
                .respond(response().withStatusCode(200).withBody(body, MediaType.APPLICATION_JSON));
    }

    private static void get(String path, String body) {
        MockServerSupport.server()
                .when(request().withMethod("GET").withPath(path))
                .respond(response().withStatusCode(200).withBody(body, MediaType.APPLICATION_JSON));
    }
}
