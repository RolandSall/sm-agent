package io.github.scrumagent.jira.internal;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the JQL fragments and search field lists the client interpolates into Jira REST calls.
 * Moved verbatim from the former JiraClient god class so every emitted string — quoting, the
 * {@code project in (...)} CSV, the requested field list — is byte-identical and the HTTP-boundary
 * stubs still match.
 */
@Component
class JqlBuilder {

    private static final List<String> BASE_SEARCH_FIELDS =
            List.of("summary", "issuetype", "status", "assignee", "labels", "issuelinks",
                    "fixVersions", "timetracking");

    private final JiraProperties props;

    JqlBuilder(JiraProperties props) {
        this.props = props;
    }

    /**
     * Quote and escape a free-text value for safe interpolation into a JQL string literal: escape
     * backslashes, then double-quotes, then wrap in double-quotes. Without this an agent-supplied
     * status name like {@code Ready") OR (1=1} would break out of the string and rewrite the query.
     */
    static String jqlQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Comma-joined, JQL-quoted project keys for {@code project in (...)}. Keys are config, not
     * agent-supplied, but they are still interpolated into JQL so they get the same quoting. */
    String projectsCsv() {
        return props.projectKeys().stream().map(JqlBuilder::jqlQuote).reduce((a, b) -> a + "," + b).orElse("");
    }

    /**
     * Sprint-issue searches must request the instance-specific custom fields explicitly, or Jira
     * returns them null — which silently blanks story points, acceptance criteria and logged hours
     * for every hygiene/worklog/risk computation. Base fields + configured field ids.
     */
    String searchFields() {
        List<String> fields = new ArrayList<>(BASE_SEARCH_FIELDS);
        fields.add(props.fields().storyPoints());
        fields.addAll(props.fields().acceptanceCriteria());
        if (props.fields().loggedHours() != null) {
            fields.add(props.fields().loggedHours());
        }
        if (props.fields().epicLink() != null) {
            fields.add(props.fields().epicLink());
        }
        return String.join(",", fields);
    }
}
