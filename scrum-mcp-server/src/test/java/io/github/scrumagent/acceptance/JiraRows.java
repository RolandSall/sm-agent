package io.github.scrumagent.acceptance;

import java.util.Map;

/**
 * Turns a Gherkin {@code DataTable} row (a {@code Map<column,value>}) into a {@link JiraJson.Issue},
 * so the acceptance scenarios can spell out their input Jira issues inline as a table instead of
 * bespoke Java. Only the columns actually present (and non-blank) on the row are applied — a missing
 * or blank cell means "field absent", exactly as Jira omits absent fields — and each is delegated to
 * the matching {@link JiraJson.Issue} builder so the wire shape stays identical to hand-built data.
 *
 * <p>Recognised columns ({@code key} required, the rest optional):
 * <ul>
 *   <li>{@code key} — the issue key</li>
 *   <li>{@code summary}, {@code assignee}, {@code type}, {@code epic} (Epic-Link), {@code ac}
 *       (acceptance criteria)</li>
 *   <li>{@code dependOn}, {@code dependents} (Dependency option fields), {@code deliverySprint}
 *       (+ optional {@code deliveryEnd} ISO instant)</li>
 *   <li>{@code status} — {@code "Name"} or {@code "Name/Category"} (category defaults to the name)</li>
 *   <li>{@code points}, {@code devJobPoints} — story-point / dev-job doubles</li>
 *   <li>{@code estimate}, {@code remaining}, {@code aggregateEstimate} — timetracking hours</li>
 *   <li>{@code logged} (native {@code timeSpent}), {@code aggregateLogged}, {@code loggedField}
 *       (the configured logged-hours custom field)</li>
 * </ul>
 *
 * <p>Hours cells accept {@code "8h"} or {@code "8"} (and negatives like {@code "-5h"} for the clamp
 * scenario); hours convert to seconds as {@code round(h * 3600)}.
 */
public final class JiraRows {

    private JiraRows() {
    }

    public static JiraJson.Issue toIssue(Map<String, String> row) {
        String key = value(row, "key");
        if (key == null) {
            throw new IllegalArgumentException("A Jira issue row requires a non-blank 'key' column: " + row);
        }
        JiraJson.Issue issue = JiraJson.Issue.issue(key);

        applyText(row, "summary", issue::summary);
        applyText(row, "assignee", issue::assignee);
        applyText(row, "type", issue::type);
        applyText(row, "epic", issue::epicLink);
        applyText(row, "ac", issue::acceptanceCriteria);
        applyText(row, "dependOn", issue::dependOn);
        applyText(row, "dependents", issue::dependents);

        String deliverySprint = value(row, "deliverySprint");
        if (deliverySprint != null) {
            issue.deliverySprint(deliverySprint, value(row, "deliveryEnd"));
        }

        String status = value(row, "status");
        if (status != null) {
            int slash = status.indexOf('/');
            String name = slash >= 0 ? status.substring(0, slash).trim() : status;
            String category = slash >= 0 ? status.substring(slash + 1).trim() : status;
            issue.status(name, category);
        }

        applyDouble(row, "points", issue::storyPoints);
        applyDouble(row, "devJobPoints", issue::devJobPoints);

        String loggedField = value(row, "loggedField");
        if (loggedField != null) {
            issue.loggedHours(hours(loggedField));
        }

        Integer estimate = value(row, "estimate") != null ? seconds(value(row, "estimate")) : null;
        Integer remaining = value(row, "remaining") != null ? seconds(value(row, "remaining")) : null;
        if (estimate != null || remaining != null) {
            issue.timetracking(estimate, remaining);
        }

        String aggregateEstimate = value(row, "aggregateEstimate");
        if (aggregateEstimate != null) {
            issue.aggregateOriginalEstimate(seconds(aggregateEstimate));
        }
        String logged = value(row, "logged");
        if (logged != null) {
            issue.timeSpent(seconds(logged));
        }
        String aggregateLogged = value(row, "aggregateLogged");
        if (aggregateLogged != null) {
            issue.aggregateTimeSpent(seconds(aggregateLogged));
        }
        return issue;
    }

    private static void applyText(Map<String, String> row, String column,
                                  java.util.function.Function<String, JiraJson.Issue> builder) {
        String v = value(row, column);
        if (v != null) {
            builder.apply(v);
        }
    }

    private static void applyDouble(Map<String, String> row, String column,
                                    java.util.function.DoubleFunction<JiraJson.Issue> builder) {
        String v = value(row, column);
        if (v != null) {
            builder.apply(Double.parseDouble(v));
        }
    }

    /** Trimmed cell value, or {@code null} when the column is absent or blank (= "field absent"). */
    private static String value(Map<String, String> row, String column) {
        String v = row.get(column);
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Parse {@code "8h"} or {@code "8"} (or a negative like {@code "-5h"}) as a number of hours. */
    private static double hours(String raw) {
        String t = raw.trim();
        if (t.endsWith("h") || t.endsWith("H")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        return Double.parseDouble(t);
    }

    /** Hours cell to seconds, {@code round(h * 3600)}. */
    private static int seconds(String raw) {
        return (int) Math.round(hours(raw) * 3600);
    }
}
