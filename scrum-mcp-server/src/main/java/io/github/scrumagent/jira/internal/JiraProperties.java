package io.github.scrumagent.jira.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Jira connection + instance-specific field mapping.
 *
 * <p>Custom field IDs are per-Jira-instance ("acceptance criteria" commonly lives
 * in customfield_10036/10024). They are configuration here, not hard-coded, to keep the
 * gateway portable across Jira instances. Find your instance's IDs
 * once via {@code GET /rest/api/2/field}.
 *
 * @param baseUrl e.g. {@code https://jira.mycompany.com}
 * @param token Personal Access Token (Server/DC Bearer auth)
 * @param projectKeys the team's Jira project(s) — a list, since a team board can span several
 *        (e.g. {@code [VAL, MXEV]}); "our" projects, so anything else is another team
 * @param boardId the Agile board id for sprint metrics; if {@code null}, resolved from the first project
 * @param featureIssueType the Jira issue type that represents a "feature" for the effort roll-up
 *        (this instance groups stories under an Epic via the Epic-Link field); defaults to {@code Epic}
 * @param insecureTls DEV-ONLY escape hatch: when {@code true}, TLS certificate validation is skipped
 *        for Jira calls. For local piloting against a corporate Jira whose CA is not in the JVM
 *        truststore (curl works via the OS store; the JVM has its own). Default {@code false}; the
 *        production fix is importing the corporate CA into a truststore, not enabling this.
 * @param workingHoursPerDay hours in one working day, used only to re-express effort hours as derived
 *        working-days; {@code null} or non-positive falls back to {@code 8.0}. Team policy, so it is
 *        configuration, not hard-coded.
 * @param teamName our team's identity value (e.g. {@code RIO}) as it appears in the Dependency
 *        {@code Depend On} / {@code Dependent/s} option fields — the anchor that tells "a team we
 *        wait on" from "a team waiting on us". Instance identity, so Tier-1 config.
 * @param dependencyIssueType the Jira issue type that models a cross-team dependency on this instance;
 *        defaults to {@code Dependency}
 * @param fields instance-specific custom field IDs
 */
@ConfigurationProperties(prefix = "scrum.jira")
public record JiraProperties(String baseUrl, String token, List<String> projectKeys, Integer boardId,
                             String sprintNameFilter, String featureIssueType, boolean insecureTls,
                             Double workingHoursPerDay, String teamName, String dependencyIssueType,
                             Fields fields) {

    /** The issue type a "feature" maps to; {@code Epic} on this instance unless configured otherwise. */
    public String featureIssueType() {
        return featureIssueType != null && !featureIssueType.isBlank() ? featureIssueType : "Epic";
    }

    /** The Dependency issue type; defaults to {@code Dependency} when unset. */
    public String dependencyIssueType() {
        return dependencyIssueType != null && !dependencyIssueType.isBlank() ? dependencyIssueType : "Dependency";
    }

    /** Hours per working day for the derived day ingredients; defaults to 8.0 when unset or non-positive. */
    public Double workingHoursPerDay() {
        return workingHoursPerDay != null && workingHoursPerDay > 0 ? workingHoursPerDay : 8.0;
    }

    /**
     * @param acceptanceCriteria candidate custom field IDs, checked in order
     * @param storyPoints the story-points estimate field ID
     * @param loggedHours the custom field holding logged work hours; if {@code null}, logged time is
     *        read from Jira's native {@code timetracking.timeSpentSeconds} instead
     * @param epicLink the field linking a story to its parent Epic ("feature"); {@code null} if unused
     * @param devJob the Epic's own "planned dev job" estimate (story points), a synced snapshot that
     *        DRIFTS from the live child sum — surfaced only as a reference ingredient; {@code null} if unused
     * @param dependOn the Dependency's "Depend On" single-select option field — the team we WAIT ON
     *        (the blocker); read {@code .value}. {@code null} if unused
     * @param dependents the Dependency's "Dependent/s" single-select option field — the team WAITING
     *        (the consumer); read {@code .value}. {@code null} if unused
     * @param deliverySprint the Dependency's delivery-sprint field (a Jira Sprint custom field, array
     *        of name+endDate elements); parsed best-effort. {@code null} if unused
     */
    public record Fields(List<String> acceptanceCriteria, String storyPoints, String loggedHours,
                         String epicLink, String devJob, String dependOn, String dependents,
                         String deliverySprint) {
    }
}
