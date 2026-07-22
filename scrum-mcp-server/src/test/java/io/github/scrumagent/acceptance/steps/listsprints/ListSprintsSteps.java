package io.github.scrumagent.acceptance.steps.listsprints;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

/** List-sprints steps: stub the board's sprints per state, drive {@code /api/sprints}, delegate to {@link ListSprintsState}. */
public class ListSprintsSteps {

    private final ScrumApiDriver apiDriver;
    private final ListSprintsState state;

    @Autowired
    public ListSprintsSteps(ScrumApiDriver apiDriver, ListSprintsState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("the board has active sprints {string} with id {long} and {string} with id {long}")
    public void boardHasActiveSprints(String nameA, long idA, String nameB, long idB) {
        JiraStubs.activeSprints(1, JiraJson.sprintListResponse(
                JiraJson.sprint(idA, nameA, "active", "2020-01-01T00:00:00.000Z", "2099-01-01T00:00:00.000Z", null),
                JiraJson.sprint(idB, nameB, "active", "2020-01-01T00:00:00.000Z", "2099-01-01T00:00:00.000Z", null)));
    }

    @Given("the board has closed sprints {string} with id {long}")
    public void boardHasClosedSprints(String name, long id) {
        JiraStubs.closedSprints(1, JiraJson.sprintListResponse(
                JiraJson.sprint(id, name, "closed", "2019-01-01T00:00:00.000Z", "2019-01-15T00:00:00.000Z",
                        "2019-01-15T00:00:00.000Z")));
    }

    @When("I list the board's sprints")
    public void listSprints() throws Exception {
        state.capture(apiDriver.sprints(null));
    }

    @When("I list the board's sprints in state {string}")
    public void listSprintsInState(String sprintState) throws Exception {
        state.capture(apiDriver.sprints(sprintState));
    }

    @Then("the sprints are")
    public void sprintsAre(DataTable table) {
        state.assertSprintsMatch(table.asMaps());
    }
}
