package io.github.scrumagent.acceptance.steps.flow;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraRows;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.github.scrumagent.acceptance.JiraJson.Issue.issue;

/**
 * Sprint-flow steps: stub the board active sprint + its issues (with changelog), drive
 * {@code /api/sprint-flow}, delegate assertions to {@link SprintFlowState}.
 *
 * <p>Feature data uses FIXED far-past timestamps (2020) rather than {@code now()}-relative dates: the
 * handler computes real date-diffs against {@code Instant.now()}, so a 2020 status change always
 * yields a large (but non-deterministic) age — hence those columns are left unasserted in the
 * feature, while {@code statusCategory}, {@code reopenCount} and null-age stay deterministic.
 */
public class SprintFlowSteps {

    /** Jira changelog timestamp format: colon-less offset, e.g. {@code 2026-07-14T10:00:00.000+0000}. */
    private static final DateTimeFormatter JIRA_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT);

    private final ScrumApiDriver apiDriver;
    private final SprintFlowState state;

    @Autowired
    public SprintFlowSteps(ScrumApiDriver apiDriver, SprintFlowState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("the active sprint has a long-idle in-progress issue, a stale issue, a reopened issue and a healthy to-do issue")
    public void mixedFlowIssues() {
        // "Healthy" needs a near-now timestamp so its age and days-since-updated stay 0 regardless of
        // when the suite runs; the others use 2020 timestamps for large, deterministic-in-sign diffs.
        String recent = OffsetDateTime.now(ZoneOffset.UTC).format(JIRA_DATETIME);
        JiraStubs.activeSprints(1, JiraJson.sprintListResponse(JiraJson.sprint(100, "RIO Test", "active",
                "2020-01-01T00:00:00.000Z", "2099-01-01T00:00:00.000Z", null)));
        JiraStubs.sprintIssues(100, JiraJson.sprintIssuesResponse(
                // In progress, only status move in 2020 -> huge age-in-status, reopenCount 0.
                issue("RIO-1").summary("Idle").status("In Development", "In Progress").assignee("Alice")
                        .statusTransition("2020-01-01T00:00:00.000+0000", "Alice", "To Do", "In Development"),
                // In progress, NO status change but updated in 2020 -> age UNKNOWN (null), reopenCount 0.
                issue("RIO-2").summary("Stale").status("In Development", "In Progress").assignee("Bob")
                        .updated("2020-01-01T00:00:00.000+0000"),
                // Moved on after reaching a done-like status (Closed -> Reopened) -> reopenCount 1.
                issue("RIO-3").summary("Churned").status("Reopened", "In Progress").assignee("Carol")
                        .statusTransition("2020-01-01T00:00:00.000+0000", "Carol", "In Development", "Closed")
                        .statusTransition("2020-01-02T00:00:00.000+0000", "Carol", "Closed", "Reopened"),
                // Healthy: To Do, updated just now, never moved status -> the old buckets excluded it.
                issue("RIO-4").summary("Healthy").status("To Do", "To Do").assignee("Dave")
                        .updated(recent)));
    }

    @Given("the active sprint has the following issues")
    public void followingIssues(DataTable table) {
        JiraStubs.activeSprints(1, JiraJson.sprintListResponse(JiraJson.sprint(100, "RIO Test", "active",
                "2020-01-01T00:00:00.000Z", "2099-01-01T00:00:00.000Z", null)));
        List<JiraJson.Issue> issues = new ArrayList<>();
        for (Map<String, String> row : table.asMaps()) {
            // Row supplies key/type; a fixed changelog is attached here (JiraRows carries no changelog).
            issues.add(JiraRows.toIssue(row)
                    .status("In Development", "In Progress")
                    .statusTransition("2020-01-01T00:00:00.000+0000", "System", "To Do", "In Development"));
        }
        JiraStubs.sprintIssues(100, JiraJson.sprintIssuesResponse(issues.toArray(new JiraJson.Issue[0])));
    }

    @When("I request the sprint-flow report")
    public void requestFlow() throws Exception {
        state.capture(apiDriver.sprintFlow(null, null));
    }

    @When("I request the sprint-flow report filtered to issue type {string}")
    public void requestFlowWithIssueType(String issueType) throws Exception {
        state.capture(apiDriver.sprintFlow(issueType, null));
    }

    @Then("the flow issues are")
    public void theFlowIssuesAre(DataTable table) {
        state.assertIssuesMatch(table.asMaps());
    }

    @Then("the flow list has {int} issue(s)")
    public void flowListCount(int expected) {
        state.assertIssueCount(expected);
    }

    @Then("issue {string} is in the flow list")
    public void inFlowList(String key) {
        state.assertInList(key);
    }

    @Then("issue {string} has a null ageInStatusDays")
    public void nullAge(String key) {
        state.assertNullAge(key);
    }
}
