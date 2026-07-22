package io.github.scrumagent.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal, valid Jira wire-format JSON builders for the acceptance tests. Only the fields the
 * {@code JiraClient} actually reads are populated; "missing" fields are simply omitted (Jira omits
 * absent fields too, and the client's {@code path(...).asText(null)} handles absence). Values are
 * assembled as maps/lists and serialized with Jackson so the output is always well-formed.
 *
 * <p>Public because the layered step definitions live in sub-packages of {@code acceptance} and
 * reuse these builders for their scenario data setup.
 */
public final class JiraJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JiraJson() {
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize test JSON", e);
        }
    }

    /** {@code /rest/api/2/search} response body wrapping the given issue nodes. */
    public static String searchResponse(Issue... issues) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Issue issue : issues) {
            nodes.add(issue.node());
        }
        return write(Map.of("issues", nodes));
    }

    /** {@code /rest/agile/1.0/board/{id}/sprint} response body ({@code values} array). */
    @SafeVarargs
    public static String sprintListResponse(Map<String, Object>... sprints) {
        return write(Map.of("values", List.of(sprints)));
    }

    /** A single Agile sprint node ({@code /rest/agile/1.0/sprint/{id}} and list entries). */
    public static Map<String, Object> sprint(long id, String name, String state, String startDate,
                                             String endDate, String completeDate) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", id);
        node.put("name", name);
        node.put("state", state);
        if (startDate != null) {
            node.put("startDate", startDate);
        }
        if (endDate != null) {
            node.put("endDate", endDate);
        }
        if (completeDate != null) {
            node.put("completeDate", completeDate);
        }
        return node;
    }

    /** {@code /rest/agile/1.0/sprint/{id}/issue} response body. */
    public static String sprintIssuesResponse(Issue... issues) {
        return searchResponse(issues);
    }

    /** Single-issue GET response body (the bare issue node, as {@code /rest/api/2/issue/{key}} returns). */
    public static String issueResponse(Issue issue) {
        return write(issue.node());
    }

    /** {@code /rest/greenhopper/1.0/rapid/charts/velocity} response body. */
    public static String velocityResponse(VelocityEntry... entries) {
        List<Map<String, Object>> sprints = new ArrayList<>();
        Map<String, Object> stats = new HashMap<>();
        for (VelocityEntry e : entries) {
            sprints.add(Map.of("id", e.id(), "name", e.name()));
            stats.put(String.valueOf(e.id()), Map.of(
                    "estimated", Map.of("value", e.committed()),
                    "completed", Map.of("value", e.completed())));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("sprints", sprints);
        body.put("velocityStatEntries", stats);
        return write(body);
    }

    public static VelocityEntry vel(long id, String name, double committed, double completed) {
        return new VelocityEntry(id, name, committed, completed);
    }

    public record VelocityEntry(long id, String name, double committed, double completed) {
    }

    /** Fluent builder for one Jira issue node (search, sprint-issue, or single-issue shapes). */
    public static final class Issue {

        private final String key;
        private final Map<String, Object> fields = new HashMap<>();
        private final Map<String, Object> renderedFields = new HashMap<>();
        private final List<Map<String, Object>> issueLinks = new ArrayList<>();
        private final List<Map<String, Object>> fixVersions = new ArrayList<>();
        private final List<Map<String, Object>> histories = new ArrayList<>();

        private Issue(String key) {
            this.key = key;
        }

        public static Issue issue(String key) {
            return new Issue(key);
        }

        public Issue summary(String summary) {
            fields.put("summary", summary);
            return this;
        }

        public Issue type(String typeName) {
            fields.put("issuetype", Map.of("name", typeName));
            return this;
        }

        /** Sets status name and its statusCategory name (e.g. "In Progress", "Done", "To Do"). */
        public Issue status(String name, String statusCategory) {
            fields.put("status", Map.of(
                    "name", name,
                    "statusCategory", Map.of("name", statusCategory)));
            return this;
        }

        public Issue assignee(String displayName) {
            fields.put("assignee", Map.of("displayName", displayName));
            return this;
        }

        /** Sets {@code fields.updated} (ISO/Jira timestamp) — drives the flow "stalled" signal. */
        public Issue updated(String iso) {
            fields.put("updated", iso);
            return this;
        }

        public Issue labels(String... labels) {
            fields.put("labels", List.of(labels));
            return this;
        }

        public Issue storyPoints(double points) {
            fields.put("customfield_10002", points);
            return this;
        }

        public Issue acceptanceCriteria(String text) {
            fields.put("customfield_10036", text);
            renderedFields.put("customfield_10036", text);
            return this;
        }

        /** Sets the Epic-Link field (test config {@code customfield_12471}) — the parent "feature" key. */
        public Issue epicLink(String epicKey) {
            fields.put("customfield_12471", epicKey);
            return this;
        }

        /** Sets the Epic's own "dev job" points snapshot (test config {@code customfield_24665}). */
        public Issue devJobPoints(double points) {
            fields.put("customfield_24665", points);
            return this;
        }

        /** Sets "Depend On" (test config {@code customfield_27471}) — the team we WAIT ON (blocker). */
        public Issue dependOn(String team) {
            fields.put("customfield_27471", Map.of("value", team));
            return this;
        }

        /** Sets "Dependent/s" (test config {@code customfield_27470}) — the team WAITING (consumer). */
        public Issue dependents(String team) {
            fields.put("customfield_27470", Map.of("value", team));
            return this;
        }

        /**
         * Sets the delivery-sprint field (test config {@code customfield_14960}) as Jira's modern
         * Sprint-custom-field shape: an array of JSON objects with {@code name}/{@code endDate}.
         * {@code endIso} may be {@code null} (endDate omitted).
         */
        public Issue deliverySprint(String name, String endIso) {
            Map<String, Object> element = new HashMap<>();
            element.put("name", name);
            if (endIso != null) {
                element.put("endDate", endIso);
            }
            fields.put("customfield_14960", List.of(element));
            return this;
        }

        /** Sets {@code aggregatetimeoriginalestimate} (seconds) — estimate incl. sub-tasks. */
        public Issue aggregateOriginalEstimate(int seconds) {
            fields.put("aggregatetimeoriginalestimate", seconds);
            return this;
        }

        /** Sets {@code aggregatetimespent} (seconds) — logged work incl. sub-tasks. */
        public Issue aggregateTimeSpent(int seconds) {
            fields.put("aggregatetimespent", seconds);
            return this;
        }

        public Issue loggedHours(double hours) {
            fields.put("customfield_10040", hours);
            return this;
        }

        public Issue timetracking(Integer originalEstimateSeconds, Integer remainingEstimateSeconds) {
            Map<String, Object> tt = timetrackingNode();
            if (originalEstimateSeconds != null) {
                tt.put("originalEstimateSeconds", originalEstimateSeconds);
            }
            if (remainingEstimateSeconds != null) {
                tt.put("remainingEstimateSeconds", remainingEstimateSeconds);
            }
            return this;
        }

        /**
         * Sets {@code timetracking.timeSpentSeconds} — Jira's native aggregate worklog, the fallback
         * the client uses when no logged-hours custom field is configured/present.
         */
        public Issue timeSpent(int timeSpentSeconds) {
            timetrackingNode().put("timeSpentSeconds", timeSpentSeconds);
            return this;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> timetrackingNode() {
            return (Map<String, Object>) fields.computeIfAbsent("timetracking", k -> new HashMap<>());
        }

        /** An outward issue link ("this issue {type} {targetKey}"), e.g. Blocks. */
        public Issue outwardLink(String type, String targetKey, String targetStatus) {
            issueLinks.add(Map.of(
                    "type", Map.of("name", type),
                    "outwardIssue", Map.of(
                            "key", targetKey,
                            "fields", Map.of("status", Map.of("name", targetStatus)))));
            return this;
        }

        /** A changelog status transition entry ({@code created} in Jira colon-less offset format). */
        public Issue statusTransition(String created, String author, String fromStatus, String toStatus) {
            histories.add(Map.of(
                    "created", created,
                    "author", Map.of("displayName", author),
                    "items", List.of(Map.of(
                            "field", "status",
                            "fromString", fromStatus,
                            "toString", toStatus))));
            return this;
        }

        /** A changelog Sprint-field transition entry (scope in/out; {@code from}/{@code to} are CSV ids). */
        public Issue sprintTransition(String created, String author, String fromIds, String toIds) {
            histories.add(Map.of(
                    "created", created,
                    "author", Map.of("displayName", author),
                    "items", List.of(Map.of(
                            "field", "Sprint",
                            "from", fromIds,
                            "to", toIds))));
            return this;
        }

        public Map<String, Object> node() {
            Map<String, Object> allFields = new HashMap<>(fields);
            allFields.put("issuelinks", issueLinks);
            allFields.put("fixVersions", fixVersions);
            if (!allFields.containsKey("labels")) {
                allFields.put("labels", List.of());
            }

            Map<String, Object> node = new HashMap<>();
            node.put("key", key);
            node.put("fields", allFields);
            node.put("renderedFields", renderedFields);
            if (!histories.isEmpty()) {
                node.put("changelog", Map.of("histories", histories));
            }
            return node;
        }
    }
}
