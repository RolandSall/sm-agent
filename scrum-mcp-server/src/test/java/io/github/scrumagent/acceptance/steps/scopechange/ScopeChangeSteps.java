package io.github.scrumagent.acceptance.steps.scopechange;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.scrumagent.acceptance.JiraJson.Issue.issue;

/**
 * Scope-change steps: stub the active sprint, its start date and its issues' Sprint-field changelog,
 * drive {@code /api/scope-change}, delegate assertions to {@link ScopeChangeState}.
 *
 * <p>Timestamps are fixed: the sprint started 2020-06-01; the add/remove transitions are dated
 * 2020-07-01, after the start, so they count as scope change rather than the original commitment.
 */
public class ScopeChangeSteps {

    private static final long SPRINT_ID = 100;

    private final ScrumApiDriver apiDriver;
    private final ScopeChangeState state;

    @Autowired
    public ScopeChangeSteps(ScrumApiDriver apiDriver, ScopeChangeState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("the active sprint started with a 5-point committed story, a 3-point story added and a 2-point story removed after start")
    public void sprintWithScopeChange() {
        JiraStubs.activeSprints(1, JiraJson.sprintListResponse(JiraJson.sprint(SPRINT_ID,
                "RIO Scope Sprint", "active",
                "2020-06-01T00:00:00.000Z", "2099-01-01T00:00:00.000Z", null)));
        JiraStubs.sprintStart(SPRINT_ID, JiraJson.write(JiraJson.sprint(SPRINT_ID, "RIO Scope Sprint",
                "active", "2020-06-01T00:00:00.000Z", "2099-01-01T00:00:00.000Z", null)));
        JiraStubs.sprintIssues(SPRINT_ID, JiraJson.sprintIssuesResponse(
                // Committed at start: in the sprint before start, no Sprint-field change.
                issue("PROJ-1").summary("Committed").status("In Development", "In Progress").storyPoints(5),
                // Added after start: moved INTO sprint 100 on 2020-07-01.
                issue("PROJ-2").summary("Added").status("To Do", "To Do").storyPoints(3)
                        .sprintTransition("2020-07-01T00:00:00.000+0000", "Alice", "", "100"),
                // Removed after start: moved OUT of sprint 100 on 2020-07-01.
                issue("PROJ-3").summary("Removed").status("To Do", "To Do").storyPoints(2)
                        .sprintTransition("2020-07-01T00:00:00.000+0000", "Alice", "100", "")));
    }

    @When("I request the scope-change report")
    public void requestScopeChange() throws Exception {
        state.capture(apiDriver.scopeChange());
    }

    @Then("the scope change is")
    public void scopeChangeIs(DataTable table) {
        state.assertScopeSummary(table.asMaps().get(0));
    }

    @Then("the scope-change items are")
    public void scopeChangeItems(DataTable table) {
        state.assertScopeItems(table.asMaps());
    }
}
