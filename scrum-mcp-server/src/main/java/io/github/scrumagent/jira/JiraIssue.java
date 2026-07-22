package io.github.scrumagent.jira;

import java.util.List;

/**
 * Curated view of a Jira issue — the only issue shape other modules (and ultimately the
 * LLM) ever see. Deliberately small: raw Jira payloads are 40KB of custom fields; this is
 * the ~dozen fields the playbooks need.
 *
 * @param assignee display name, or {@code null} when unassigned
 * @param acceptanceCriteria extracted from the configured custom fields, or {@code null}
 * @param epicKey the parent Epic ("feature") key from the configured Epic-Link field, or {@code null}
 * @param storyPoints from the configured story-points field, or {@code null} if unestimated
 * @param links issue links — the raw material of the dependency radar
 * @param fixVersions target release names from {@code fixVersions}, empty if none
 */
public record JiraIssue(
        String key,
        String summary,
        String type,
        String status,
        String statusCategory,
        String assignee,
        List<String> labels,
        String description,
        String acceptanceCriteria,
        String epicKey,
        Double storyPoints,
        List<IssueLink> links,
        List<String> fixVersions) {

    /**
     * @param type link type name, e.g. {@code Blocks}
     * @param direction {@code inward} ("is blocked by") or {@code outward} ("blocks")
     */
    public record IssueLink(String type, String direction, String key, String status) {
    }
}
