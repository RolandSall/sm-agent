package io.github.scrumagent.jira;

import io.github.scrumagent.shared.StatusChange;

import java.util.List;

/**
 * A linked issue plus its status-transition timeline (from Jira's changelog). This is how the
 * dependency radar answers "what happened to this dependency" — the {@code history} is ordered
 * oldest-first.
 */
public record JiraIssueHistory(JiraIssue issue, List<StatusChange> history) {
}
