package io.github.scrumagent.acceptance.steps.hygiene;

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
 * Board-hygiene steps: {@code Given} stubs the active sprint on MockServer, {@code When} drives the
 * read API via the shared driver, {@code Then} delegates every assertion to {@link HygieneState}.
 * Contains no assertion logic.
 */
public class HygieneSteps {

    private final ScrumApiDriver apiDriver;
    private final HygieneState state;

    @Autowired
    public HygieneSteps(ScrumApiDriver apiDriver, HygieneState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("the active sprint has one clean issue plus issues missing estimate, acceptance criteria and assignee")
    public void mixedHygiene() {
        JiraStubs.activeSprintSearch(JiraJson.searchResponse(
                issue("PROJ-1").summary("Clean").type("Story").status("To Do", "To Do")
                        .assignee("Alice").storyPoints(5).acceptanceCriteria("Given/When/Then"),
                issue("PROJ-2").summary("No estimate").type("Story").status("To Do", "To Do")
                        .assignee("Alice").acceptanceCriteria("Given/When/Then"),
                issue("PROJ-3").summary("No AC").type("Story").status("To Do", "To Do")
                        .assignee("Alice").storyPoints(3),
                issue("PROJ-4").summary("No assignee").type("Story").status("To Do", "To Do")
                        .storyPoints(2).acceptanceCriteria("Given/When/Then")));
    }

    @Given("the active sprint has only fully-populated issues")
    public void allGreen() {
        JiraStubs.activeSprintSearch(JiraJson.searchResponse(
                issue("PROJ-1").summary("Green one").type("Story").status("To Do", "To Do")
                        .assignee("Alice").storyPoints(5).acceptanceCriteria("Given/When/Then"),
                issue("PROJ-2").summary("Green two").type("Story").status("In Progress", "In Progress")
                        .assignee("Bob").storyPoints(3).acceptanceCriteria("Given/When/Then")));
    }

    @When("I request the board hygiene report")
    public void requestHygiene() throws Exception {
        state.capture(apiDriver.hygiene(null, null));
    }

    @Then("the hygiene per issue is")
    public void hygienePerIssue(DataTable table) {
        state.assertHygienePerIssue(table.asMaps());
    }

    @Then("the hygiene totals are")
    public void hygieneTotals(DataTable table) {
        state.assertHygieneTotals(table.asMaps().get(0));
    }
}
