package io.github.scrumagent.acceptance.steps.wip;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.scrumagent.acceptance.JiraJson.Issue.issue;

/** WIP-limit steps: stub the active sprint, drive {@code /api/wip}, delegate assertions to {@link WipState}. */
public class WipSteps {

    private final ScrumApiDriver apiDriver;
    private final WipState state;

    @Autowired
    public WipSteps(ScrumApiDriver apiDriver, WipState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("the active sprint has Bob with 3 in-progress issues and Alice with 1 in-progress issue")
    public void bobAndAliceInProgress() {
        JiraStubs.activeSprintSearch(JiraJson.searchResponse(
                issue("PROJ-1").summary("B1").status("In Development", "In Progress").assignee("Bob"),
                issue("PROJ-2").summary("B2").status("In Development", "In Progress").assignee("Bob"),
                issue("PROJ-3").summary("B3").status("In Development", "In Progress").assignee("Bob"),
                issue("PROJ-4").summary("A1").status("In Development", "In Progress").assignee("Alice")));
    }

    @Given("the active sprint has 1 in-progress issue plus a To Do and a Done issue")
    public void oneInProgressPlusExcluded() {
        JiraStubs.activeSprintSearch(JiraJson.searchResponse(
                issue("PROJ-1").summary("Doing").status("In Development", "In Progress").assignee("Bob"),
                issue("PROJ-2").summary("Todo").status("To Do", "To Do").assignee("Bob"),
                issue("PROJ-3").summary("Done").status("Closed", "Done").assignee("Bob")));
    }

    @Given("the active sprint has 2 in-progress Stories and 1 in-progress Bug")
    public void mixedTypesInProgress() {
        JiraStubs.activeSprintSearch(JiraJson.searchResponse(
                issue("PROJ-1").summary("S1").type("Story").status("In Development", "In Progress").assignee("Alice"),
                issue("PROJ-2").summary("S2").type("Story").status("In Development", "In Progress").assignee("Bob"),
                issue("PROJ-3").summary("B1").type("Bug").status("In Development", "In Progress").assignee("Bob")));
    }

    @Given("there is no active sprint")
    public void noActiveSprint() {
        JiraStubs.emptyActiveSprint();
    }

    @Given("the active sprint has no issues")
    public void activeSprintNoIssues() {
        JiraStubs.activeSprintSearch(JiraJson.searchResponse());
    }

    @When("I request the WIP status with a per-assignee limit of {int}")
    public void requestWip(int perAssigneeLimit) throws Exception {
        state.capture(apiDriver.wip(perAssigneeLimit, null, null, null));
    }

    @When("I request the WIP status with a team limit of {int}")
    public void requestWipTeamLimit(int teamLimit) throws Exception {
        state.capture(apiDriver.wip(null, teamLimit, null, null));
    }

    @When("I request the WIP status filtered to issue type {string}")
    public void requestWipByType(String issueType) throws Exception {
        state.capture(apiDriver.wip(null, null, issueType, null));
    }

    @Then("the WIP per assignee is")
    public void wipPerAssignee(DataTable table) {
        state.assertWipPerAssignee(table.asMaps());
    }

    @Then("the team WIP is")
    public void teamWip(DataTable table) {
        state.assertTeamWip(table.asMaps().get(0));
    }

    @Then("no assignees are reported")
    public void noAssignees() {
        state.assertNoAssignees();
    }
}
