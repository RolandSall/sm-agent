package io.github.scrumagent.jira.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.scrumagent.jira.JiraSprint;
import io.github.scrumagent.jira.SprintIssue;
import io.github.scrumagent.jira.SprintIssueFlow;
import io.github.scrumagent.jira.StoryEffort;
import io.github.scrumagent.shared.StatusCategory;
import io.github.scrumagent.shared.WorkingDays;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.github.scrumagent.jira.internal.JiraJsonUtil.clampLogged;
import static io.github.scrumagent.jira.internal.JiraJsonUtil.csvContains;
import static io.github.scrumagent.jira.internal.JiraJsonUtil.firstNumber;
import static io.github.scrumagent.jira.internal.JiraJsonUtil.hours;
import static io.github.scrumagent.jira.internal.JiraTime.daysBetween;
import static io.github.scrumagent.jira.internal.JiraTime.parseInstant;

/**
 * Curates the Agile sprint / sprint-issue wire format into {@link JiraSprint}, {@link SprintIssue},
 * {@link SprintIssueFlow} and {@link StoryEffort} records, including the changelog-derived flow
 * metrics (done-date, scope change, age, reopen count). Moved verbatim from the former JiraClient
 * god class so every derived number is unchanged.
 */
@Component
class SprintMapper {

    /** Done-like status names — used to detect an issue being reopened after it had reached "done". */
    private static final List<String> DONE_LIKE =
            List.of("closed", "done", "resolved", "released", "cancel", "solved", "approved");

    private final JiraProperties props;

    SprintMapper(JiraProperties props) {
        this.props = props;
    }

    JiraSprint toSprint(JsonNode sprint) {
        return new JiraSprint(
                sprint.path("id").asLong(),
                sprint.path("name").asText(null),
                sprint.path("state").asText(null),
                parseInstant(sprint.path("startDate").asText(null)),
                parseInstant(sprint.path("endDate").asText(null)),
                parseInstant(sprint.path("completeDate").asText(null)),
                sprint.path("goal").asText(null));
    }

    SprintIssue toSprintIssue(JsonNode issue, long sprintId, Instant sprintStart) {
        JsonNode fields = issue.path("fields");
        String statusCategory = fields.path("status").path("statusCategory").path("name").asText(null);
        JsonNode histories = issue.path("changelog").path("histories");
        return new SprintIssue(
                issue.path("key").asText(),
                storyPoints(fields),
                fields.path("status").path("name").asText(null),
                statusCategory,
                doneDate(histories, statusCategory),
                sprintScopeChange(histories, sprintId, sprintStart, true),
                sprintScopeChange(histories, sprintId, sprintStart, false));
    }

    SprintIssueFlow toSprintFlow(JsonNode issue) {
        JsonNode fields = issue.path("fields");
        Instant now = Instant.now();

        Instant updated = parseInstant(fields.path("updated").asText(null));
        Long daysSinceUpdated = updated == null ? null : daysBetween(updated, now);

        // Collect every status change (timestamp + the status it moved TO), oldest-first.
        List<StatusTransition> transitions = new ArrayList<>();
        for (JsonNode history : issue.path("changelog").path("histories")) {
            Instant when = parseInstant(history.path("created").asText(null));
            for (JsonNode item : history.path("items")) {
                if ("status".equals(item.path("field").asText()) && when != null) {
                    transitions.add(new StatusTransition(when, item.path("toString").asText(null)));
                }
            }
        }
        transitions.sort((a, b) -> a.when().compareTo(b.when()));

        Long ageInStatusDays = transitions.isEmpty()
                ? null : daysBetween(transitions.get(transitions.size() - 1).when(), now);
        int reopenCount = reopenCount(transitions);

        return new SprintIssueFlow(
                issue.path("key").asText(),
                fields.path("summary").asText(null),
                fields.path("issuetype").path("name").asText(null),
                fields.path("assignee").path("displayName").asText(null),
                fields.path("status").path("name").asText(null),
                fields.path("status").path("statusCategory").path("name").asText(null),
                ageInStatusDays,
                daysSinceUpdated,
                reopenCount);
    }

    StoryEffort toStoryEffort(JsonNode issue) {
        String key = issue.path("key").asText();
        JsonNode fields = issue.path("fields");
        JsonNode timetracking = fields.path("timetracking");
        // Prefer the aggregate fields so a story's sub-task time is included; fall back to its own.
        Double estimateHours = hours(firstNumber(
                fields.path("aggregatetimeoriginalestimate"),
                timetracking.path("originalEstimateSeconds")));
        Double loggedHours = clampLogged(key, hours(firstNumber(
                fields.path("aggregatetimespent"),
                timetracking.path("timeSpentSeconds"))));
        double whpd = props.workingHoursPerDay();
        return new StoryEffort(
                key,
                fields.path("status").path("name").asText(null),
                storyPoints(fields),
                estimateHours,
                loggedHours,
                estimateHours == null ? 0.0 : WorkingDays.fromHours(estimateHours, whpd),
                loggedHours == null ? 0.0 : WorkingDays.fromHours(loggedHours, whpd));
    }

    private Double storyPoints(JsonNode fields) {
        JsonNode node = fields.path(props.fields().storyPoints());
        return node.isNumber() ? node.asDouble() : null;
    }

    /**
     * Reopens = status changes made after the issue FIRST reached a done-like status. If it never
     * reached one, there is nothing to reopen (0). Transitions must be oldest-first.
     */
    private static int reopenCount(List<StatusTransition> transitions) {
        Instant firstDone = null;
        for (StatusTransition t : transitions) {
            if (isDoneLike(t.toStatus())) {
                firstDone = t.when();
                break;
            }
        }
        if (firstDone == null) {
            return 0;
        }
        int count = 0;
        for (StatusTransition t : transitions) {
            if (t.when().isAfter(firstDone)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isDoneLike(String status) {
        if (status == null) {
            return false;
        }
        String lower = status.toLowerCase(Locale.ROOT);
        return DONE_LIKE.stream().anyMatch(lower::contains);
    }

    /** One status transition: when it happened and the status it moved to. */
    private record StatusTransition(Instant when, String toStatus) {
    }

    /** A completed issue's done-date = the timestamp of its last status transition. */
    private Instant doneDate(JsonNode histories, String statusCategory) {
        if (!StatusCategory.DONE.equalsIgnoreCase(statusCategory)) {
            return null;
        }
        Instant latest = null;
        for (JsonNode history : histories) {
            for (JsonNode item : history.path("items")) {
                if ("status".equals(item.path("field").asText())) {
                    Instant when = parseInstant(history.path("created").asText(null));
                    if (when != null && (latest == null || when.isAfter(latest))) {
                        latest = when;
                    }
                }
            }
        }
        return latest;
    }

    /**
     * Detect this issue joining ({@code added=true}) or leaving ({@code added=false}) the sprint
     * after it started, from the Sprint-field changelog. "Added after start" ignores changes at or
     * before the sprint start (that is the original commitment, not scope creep).
     */
    private boolean sprintScopeChange(JsonNode histories, long sprintId, Instant sprintStart, boolean added) {
        String id = String.valueOf(sprintId);
        for (JsonNode history : histories) {
            for (JsonNode item : history.path("items")) {
                if (!"Sprint".equalsIgnoreCase(item.path("field").asText())) {
                    continue;
                }
                boolean inTo = csvContains(item.path("to").asText(""), id);
                boolean inFrom = csvContains(item.path("from").asText(""), id);
                if (added) {
                    Instant when = parseInstant(history.path("created").asText(null));
                    boolean afterStart = sprintStart == null || when == null || when.isAfter(sprintStart);
                    if (inTo && !inFrom && afterStart) {
                        return true;
                    }
                } else if (inFrom && !inTo) {
                    return true;
                }
            }
        }
        return false;
    }
}
