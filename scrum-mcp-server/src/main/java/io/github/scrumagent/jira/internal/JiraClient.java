package io.github.scrumagent.jira.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.scrumagent.jira.FeatureEffort;
import io.github.scrumagent.jira.JiraGateway;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.jira.JiraIssueHistory;
import io.github.scrumagent.jira.JiraSprint;
import io.github.scrumagent.jira.JiraWorkTime;
import io.github.scrumagent.jira.SprintIssue;
import io.github.scrumagent.jira.SprintIssueFlow;
import io.github.scrumagent.jira.SprintVelocity;
import io.github.scrumagent.jira.StoryEffort;
import io.github.scrumagent.jira.TeamDependency;
import io.github.scrumagent.shared.WorkingDays;
import static io.github.scrumagent.jira.internal.JiraJsonUtil.EMPTY;
import static io.github.scrumagent.jira.internal.JiraHttp.debugIfChangelogTruncated;
import static io.github.scrumagent.jira.internal.JiraHttp.orEmpty;
import static io.github.scrumagent.jira.internal.JiraHttp.requireBody;
import static io.github.scrumagent.jira.internal.JiraHttp.warnIfTruncated;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Jira Server/DC gateway (Bearer PAT auth — {@code /rest/api/2} + {@code /rest/agile/1.0}).
 *
 * <p>Thin orchestrator implementing {@link JiraGateway}: it builds the query via {@link JqlBuilder},
 * issues the request through {@link JiraHttp}, and curates the response through {@link IssueMapper} /
 * {@link SprintMapper}, resolving board/sprint context via {@link BoardLocator}. Everything it returns
 * is already curated (records in {@code io.github.scrumagent.jira}) so no raw payload ever crosses the
 * module boundary. The wire format itself lives in the {@code jira.internal} collaborators.
 */
@Component
class JiraClient implements JiraGateway {

    /** JQL page size for issue searches; also the sprint-issue fetch cap. */
    private static final int SEARCH_PAGE_SIZE = 200;

    private final JiraHttp http;
    private final JiraProperties props;
    private final JqlBuilder jqlBuilder;
    private final IssueMapper issueMapper;
    private final SprintMapper sprintMapper;
    private final BoardLocator boardLocator;

    JiraClient(JiraProperties props, JqlBuilder jqlBuilder, JiraHttp http,
               IssueMapper issueMapper, SprintMapper sprintMapper, BoardLocator boardLocator) {
        this.props = props;
        this.jqlBuilder = jqlBuilder;
        this.http = http;
        this.issueMapper = issueMapper;
        this.sprintMapper = sprintMapper;
        this.boardLocator = boardLocator;
    }

    @Override
    public JiraIssue getIssue(String key) {
        JsonNode json = http.get(uri -> uri.path("/rest/api/2/issue/{key}")
                        .queryParam("expand", "renderedFields")
                        .build(key));
        return issueMapper.toIssue(requireBody(json, "issue/" + key), true);
    }

    @Override
    public List<JiraIssue> activeSprintIssues() {
        JiraSprint sprint = activeSprint();
        return sprint == null ? List.of() : search("sprint = " + sprint.id());
    }

    @Override
    public JiraSprint activeSprint() {
        return boardLocator.activeSprint();
    }

    @Override
    public List<JiraIssue> issuesInStatuses(List<String> statuses) {
        String quotedStatuses = statuses.stream().map(JqlBuilder::jqlQuote)
                .reduce((a, b) -> a + ", " + b).orElse("");
        return search("project in (%s) AND status in (%s)".formatted(jqlBuilder.projectsCsv(), quotedStatuses));
    }

    @Override
    public List<JiraWorkTime> activeSprintWorkTime() {
        JiraSprint sprint = activeSprint();
        if (sprint == null) {
            return List.of();
        }
        JsonNode json = searchRaw("sprint = " + sprint.id());
        List<JiraWorkTime> out = new ArrayList<>();
        for (JsonNode issue : json.path("issues")) {
            out.add(issueMapper.toWorkTime(issue));
        }
        return out;
    }

    @Override
    public List<JiraSprint> sprints(String state) {
        return boardLocator.sprints(state);
    }

    @Override
    public List<SprintVelocity> velocity() {
        int boardId = boardLocator.resolveBoardId();
        // greenhopper is Jira's internal chart API (unofficial but stable on Server/DC); it returns
        // the same numbers as the board's Velocity report, so we surface them rather than recompute.
        JsonNode json = http.get(uri -> uri.path("/rest/greenhopper/1.0/rapid/charts/velocity")
                        .queryParam("rapidViewId", boardId)
                        .build());
        json = requireBody(json, "velocity chart (rapidViewId=" + boardId + ")");
        JsonNode stats = json.path("velocityStatEntries");
        List<SprintVelocity> out = new ArrayList<>();
        for (JsonNode sprint : json.path("sprints")) {
            JsonNode entry = stats.path(sprint.path("id").asText());
            out.add(new SprintVelocity(
                    sprint.path("name").asText(null),
                    entry.path("estimated").path("value").asDouble(0),
                    entry.path("completed").path("value").asDouble(0)));
        }
        return out;
    }

    @Override
    public List<SprintIssue> sprintIssues(long sprintId) {
        Instant sprintStart = boardLocator.sprintStart(sprintId);
        JsonNode json = orEmpty(http.get(uri -> uri.path("/rest/agile/1.0/sprint/{id}/issue")
                        .queryParam("expand", "changelog")
                        .queryParam("fields", props.fields().storyPoints() + ",status")
                        .queryParam("maxResults", SEARCH_PAGE_SIZE)
                        .build(sprintId)));
        warnIfTruncated(json, "issues", SEARCH_PAGE_SIZE, "sprint/" + sprintId + "/issue");
        List<SprintIssue> out = new ArrayList<>();
        for (JsonNode issue : json.path("issues")) {
            out.add(sprintMapper.toSprintIssue(issue, sprintId, sprintStart));
        }
        return out;
    }

    @Override
    public List<SprintIssueFlow> activeSprintFlow() {
        JiraSprint sprint = activeSprint();
        if (sprint == null) {
            return List.of();
        }
        JsonNode json = orEmpty(http.get(uri -> uri.path("/rest/agile/1.0/sprint/{id}/issue")
                        .queryParam("expand", "changelog")
                        .queryParam("fields", "summary,issuetype,assignee,status,updated")
                        .queryParam("maxResults", SEARCH_PAGE_SIZE)
                        .build(sprint.id())));
        warnIfTruncated(json, "issues", SEARCH_PAGE_SIZE, "sprint/" + sprint.id() + "/issue (flow)");
        List<SprintIssueFlow> out = new ArrayList<>();
        for (JsonNode issue : json.path("issues")) {
            out.add(sprintMapper.toSprintFlow(issue));
        }
        return out;
    }

    @Override
    public List<FeatureEffort> featureEffort(List<String> epicKeys) {
        List<String> keys = (epicKeys == null || epicKeys.isEmpty())
                ? epicKeysFromActiveSprint() : epicKeys;
        List<FeatureEffort> out = new ArrayList<>();
        for (String epicKey : keys) {
            out.add(buildFeatureEffort(epicKey));
        }
        return out;
    }

    /** Distinct, non-null Epic-Links of the active sprint's issues (searchFields already requests it). */
    private List<String> epicKeysFromActiveSprint() {
        Set<String> keys = new LinkedHashSet<>();
        for (JiraIssue issue : activeSprintIssues()) {
            if (issue.epicKey() != null && !issue.epicKey().isBlank()) {
                keys.add(issue.epicKey());
            }
        }
        return new ArrayList<>(keys);
    }

    private FeatureEffort buildFeatureEffort(String epicKey) {
        // (a) the Epic itself — summary + its own dev-job snapshot (reference only, drift-prone).
        JsonNode epic = requireBody(http.get(uri -> uri.path("/rest/api/2/issue/{key}")
                        .queryParam("fields", "summary," + props.fields().devJob())
                        .build(epicKey)), "issue/" + epicKey + " (epic)");
        JsonNode epicFields = epic.path("fields");
        String epicSummary = epicFields.path("summary").asText(null);
        JsonNode devJobNode = props.fields().devJob() == null
                ? EMPTY : epicFields.path(props.fields().devJob());
        Double devJobPoints = devJobNode.isNumber() ? devJobNode.asDouble() : null;

        // (b) the Epic's children via the Epic-Link JQL, requesting effort fields explicitly.
        JsonNode json = orEmpty(http.get(uri -> uri.path("/rest/api/2/search")
                        .queryParam("jql", "\"Epic Link\" = " + JqlBuilder.jqlQuote(epicKey))
                        .queryParam("fields", "summary,status," + props.fields().storyPoints()
                                + ",timetracking,aggregatetimespent,aggregatetimeoriginalestimate")
                        .queryParam("maxResults", SEARCH_PAGE_SIZE)
                        .build()));
        warnIfTruncated(json, "issues", SEARCH_PAGE_SIZE, "epic children: " + epicKey);

        List<StoryEffort> stories = new ArrayList<>();
        for (JsonNode issue : json.path("issues")) {
            stories.add(sprintMapper.toStoryEffort(issue));
        }
        double sumPoints = stories.stream().mapToDouble(s -> s.points() == null ? 0 : s.points()).sum();
        double sumEstimate = stories.stream().mapToDouble(s -> s.estimateHours() == null ? 0 : s.estimateHours()).sum();
        double sumLogged = stories.stream().mapToDouble(s -> s.loggedHours() == null ? 0 : s.loggedHours()).sum();
        double whpd = props.workingHoursPerDay();
        return new FeatureEffort(epicKey, epicSummary, devJobPoints, stories.size(),
                sumPoints, sumEstimate, sumLogged,
                WorkingDays.fromHours(sumEstimate, whpd), WorkingDays.fromHours(sumLogged, whpd), stories);
    }

    @Override
    public List<TeamDependency> teamDependencies(List<String> epicKeys) {
        if (epicKeys == null || epicKeys.isEmpty()) {
            return List.of();
        }
        String quotedEpics = epicKeys.stream().map(JqlBuilder::jqlQuote)
                .reduce((a, b) -> a + ", " + b).orElse("");
        String jql = "project in (%s) AND issuetype = %s AND \"Epic Link\" in (%s)".formatted(
                jqlBuilder.projectsCsv(), JqlBuilder.jqlQuote(props.dependencyIssueType()), quotedEpics);
        String fields = String.join(",", "summary", "status", props.fields().dependOn(),
                props.fields().dependents(), props.fields().deliverySprint(), props.fields().epicLink());
        JsonNode json = orEmpty(http.get(uri -> uri.path("/rest/api/2/search")
                        .queryParam("jql", jql)
                        .queryParam("fields", fields)
                        .queryParam("maxResults", SEARCH_PAGE_SIZE)
                        .build()));
        warnIfTruncated(json, "issues", SEARCH_PAGE_SIZE, "dependencies for epics: " + epicKeys);
        List<TeamDependency> out = new ArrayList<>();
        for (JsonNode issue : json.path("issues")) {
            out.add(issueMapper.toTeamDependency(issue));
        }
        return out;
    }

    @Override
    public String teamName() {
        return props.teamName();
    }

    @Override
    public List<String> projectKeys() {
        return props.projectKeys();
    }

    @Override
    public double workingHoursPerDay() {
        return props.workingHoursPerDay();
    }

    @Override
    public JiraIssueHistory getIssueHistory(String key) {
        JsonNode json = http.get(uri -> uri.path("/rest/api/2/issue/{key}")
                        .queryParam("expand", "renderedFields,changelog")
                        .build(key));
        json = requireBody(json, "issue/" + key + " (history)");
        debugIfChangelogTruncated(json.path("changelog"), key);
        return new JiraIssueHistory(issueMapper.toIssue(json, true), issueMapper.statusHistory(json.path("changelog")));
    }

    // --- search helpers ---

    private List<JiraIssue> search(String jql) {
        JsonNode json = searchRaw(jql);
        List<JiraIssue> issues = new ArrayList<>();
        for (JsonNode issue : json.path("issues")) {
            issues.add(issueMapper.toIssue(issue, false));
        }
        return issues;
    }

    private JsonNode searchRaw(String jql) {
        JsonNode json = orEmpty(http.get(uri -> uri.path("/rest/api/2/search")
                        .queryParam("jql", jql)
                        .queryParam("fields", jqlBuilder.searchFields())
                        .queryParam("maxResults", SEARCH_PAGE_SIZE)
                        .build()));
        warnIfTruncated(json, "issues", SEARCH_PAGE_SIZE, "search: " + jql);
        return json;
    }

}
