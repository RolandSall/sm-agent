package io.github.scrumagent.jira.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.scrumagent.jira.JiraIssue;
import io.github.scrumagent.jira.JiraWorkTime;
import io.github.scrumagent.jira.TeamDependency;
import io.github.scrumagent.shared.StatusChange;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.scrumagent.jira.internal.JiraJsonUtil.clampLogged;
import static io.github.scrumagent.jira.internal.JiraJsonUtil.firstNonBlank;
import static io.github.scrumagent.jira.internal.JiraJsonUtil.hours;
import static io.github.scrumagent.jira.internal.JiraJsonUtil.names;
import static io.github.scrumagent.jira.internal.JiraJsonUtil.textList;
import static io.github.scrumagent.jira.internal.JiraTime.parseOffset;

/**
 * Curates the {@code /rest/api/2/issue} + search wire format into the module's {@link JiraIssue} /
 * {@link JiraWorkTime} records. Field extraction is instance-specific (custom field IDs come from
 * {@link JiraProperties}). Moved verbatim from the former JiraClient god class.
 */
@Component
class IssueMapper {

    private final JiraProperties props;

    IssueMapper(JiraProperties props) {
        this.props = props;
    }

    JiraIssue toIssue(JsonNode issue, boolean withDetails) {
        JsonNode fields = issue.path("fields");
        JsonNode rendered = issue.path("renderedFields");

        return new JiraIssue(
                issue.path("key").asText(),
                fields.path("summary").asText(null),
                fields.path("issuetype").path("name").asText(null),
                fields.path("status").path("name").asText(null),
                fields.path("status").path("statusCategory").path("name").asText(null),
                fields.path("assignee").path("displayName").asText(null),
                textList(fields.path("labels")),
                withDetails ? firstNonBlank(rendered.path("description"), fields.path("description")) : null,
                // AC is requested in the search field list too, so populate it in both paths;
                // only the rendered HTML description needs expand=renderedFields (withDetails).
                acceptanceCriteria(rendered, fields),
                epicKey(fields),
                storyPoints(fields),
                links(fields.path("issuelinks")),
                names(fields.path("fixVersions")));
    }

    JiraWorkTime toWorkTime(JsonNode issue) {
        String key = issue.path("key").asText();
        JsonNode fields = issue.path("fields");
        JsonNode timetracking = fields.path("timetracking");
        // Prefer the configured custom field; if none (this instance uses native worklog), fall back
        // to Jira's aggregate timeSpentSeconds.
        Double logged = loggedHours(fields);
        if (logged == null) {
            logged = hours(timetracking.path("timeSpentSeconds"));
        }
        logged = clampLogged(key, logged);
        return new JiraWorkTime(
                key,
                fields.path("issuetype").path("name").asText(null),
                fields.path("assignee").path("displayName").asText(null),
                fields.path("status").path("name").asText(null),
                fields.path("status").path("statusCategory").path("name").asText(null),
                logged,
                hours(timetracking.path("originalEstimateSeconds")),
                hours(timetracking.path("remainingEstimateSeconds")));
    }

    /**
     * Curates a {@code Dependency}-type issue into a {@link TeamDependency}. All reads are null-safe:
     * absent option fields yield {@code null}, and the delivery-sprint field is parsed best-effort
     * (it may be an array of JSON objects or the legacy greenhopper string form).
     */
    TeamDependency toTeamDependency(JsonNode issue) {
        JsonNode fields = issue.path("fields");
        SprintRef sprint = deliverySprint(fields.path(props.fields().deliverySprint()));
        return new TeamDependency(
                issue.path("key").asText(),
                fields.path("summary").asText(null),
                fields.path("status").path("name").asText(null),
                fields.path("status").path("statusCategory").path("name").asText(null),
                optionValue(fields.path(props.fields().dependOn())),
                optionValue(fields.path(props.fields().dependents())),
                epicKey(fields),
                sprint.name(),
                sprint.end());
    }

    /** Read a single-select custom field's {@code .value}; tolerate a plain string; {@code null} if absent. */
    private static String optionValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            return node.path("value").asText(null);
        }
        String text = node.asText(null);
        return text == null || text.isBlank() ? null : text;
    }

    private static final Pattern LEGACY_SPRINT_FIELD = Pattern.compile("(\\w+)=([^,\\]]*)");

    /** Best-effort name + end instant of the delivery sprint from Jira's Sprint custom field array. */
    private static SprintRef deliverySprint(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return new SprintRef(null, null);
        }
        // Jira appends sprints; the last element is the most recent/current target.
        JsonNode element = node.get(node.size() - 1);
        String name;
        String end;
        if (element.isObject()) {
            name = element.path("name").asText(null);
            end = element.path("endDate").asText(null);
        } else {
            // Legacy form: com.atlassian.greenhopper...Sprint@x[id=..,name=RIO Sprint 5,endDate=..Z,..]
            String raw = element.asText("");
            name = legacyField(raw, "name");
            end = legacyField(raw, "endDate");
        }
        String cleanName = name == null || name.isBlank() || "<null>".equals(name) ? null : name;
        return new SprintRef(cleanName, JiraTime.parseInstant(end));
    }

    private static String legacyField(String raw, String key) {
        Matcher m = LEGACY_SPRINT_FIELD.matcher(raw);
        while (m.find()) {
            if (key.equals(m.group(1))) {
                return m.group(2);
            }
        }
        return null;
    }

    private record SprintRef(String name, Instant end) {
    }

    /** Acceptance criteria live in instance-specific custom fields; rendered wins over raw. */
    private String acceptanceCriteria(JsonNode rendered, JsonNode fields) {
        for (String fieldId : props.fields().acceptanceCriteria()) {
            String value = firstNonBlank(rendered.path(fieldId), fields.path(fieldId));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    Double storyPoints(JsonNode fields) {
        JsonNode node = fields.path(props.fields().storyPoints());
        return node.isNumber() ? node.asDouble() : null;
    }

    /** The parent Epic ("feature") key from the configured Epic-Link field; {@code null} if unset/absent. */
    private String epicKey(JsonNode fields) {
        String field = props.fields().epicLink();
        return field == null ? null : fields.path(field).asText(null);
    }

    private Double loggedHours(JsonNode fields) {
        if (props.fields().loggedHours() == null) {
            return null;
        }
        JsonNode node = fields.path(props.fields().loggedHours());
        return node.isNumber() ? node.asDouble() : null;
    }

    private List<JiraIssue.IssueLink> links(JsonNode issuelinks) {
        List<JiraIssue.IssueLink> links = new ArrayList<>();
        for (JsonNode link : issuelinks) {
            String type = link.path("type").path("name").asText(null);
            JsonNode outward = link.path("outwardIssue");
            JsonNode inward = link.path("inwardIssue");
            if (!outward.isMissingNode()) {
                links.add(new JiraIssue.IssueLink(type, "outward",
                        outward.path("key").asText(),
                        outward.path("fields").path("status").path("name").asText(null)));
            }
            if (!inward.isMissingNode()) {
                links.add(new JiraIssue.IssueLink(type, "inward",
                        inward.path("key").asText(),
                        inward.path("fields").path("status").path("name").asText(null)));
            }
        }
        return links;
    }

    /** Ordered oldest-first list of status transitions from a changelog node. */
    List<StatusChange> statusHistory(JsonNode changelog) {
        List<StatusChange> changes = new ArrayList<>();
        for (JsonNode history : changelog.path("histories")) {
            OffsetDateTime when = parseOffset(history.path("created").asText(null));
            String author = history.path("author").path("displayName").asText(null);
            for (JsonNode item : history.path("items")) {
                if ("status".equals(item.path("field").asText())) {
                    changes.add(new StatusChange(
                            item.path("fromString").asText(null),
                            item.path("toString").asText(null),
                            when,
                            author));
                }
            }
        }
        changes.sort((a, b) -> {
            if (a.when() == null || b.when() == null) {
                return 0;
            }
            return a.when().compareTo(b.when());
        });
        return changes;
    }
}
