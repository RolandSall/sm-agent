package io.github.scrumagent.acceptance.steps.releasequeue;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static io.github.scrumagent.acceptance.JiraJson.Issue.issue;

/** Release-queue steps: stub the release search, drive {@code /api/release-queue}, delegate to {@link ReleaseQueueState}. */
public class ReleaseQueueSteps {

    private final ScrumApiDriver apiDriver;
    private final ReleaseQueueState state;

    @Autowired
    public ReleaseQueueSteps(ScrumApiDriver apiDriver, ReleaseQueueState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("the project has 2 issues {string} and 1 issue {string}")
    public void issuesInStatuses(String statusA, String statusB) {
        JiraStubs.releaseQueueSearch(JiraJson.searchResponse(
                issue("PROJ-1").summary("R1").status(statusA, "In Progress").assignee("Alice"),
                issue("PROJ-2").summary("R2").status(statusA, "In Progress").assignee("Bob"),
                issue("PROJ-3").summary("R3").status(statusB, "In Progress").assignee("Alice")));
    }

    @Given("the project has no issues in the release statuses")
    public void noReleaseIssues() {
        JiraStubs.releaseQueueSearch(JiraJson.searchResponse());
    }

    @When("I request the release queue for statuses {string} and {string}")
    public void requestReleaseQueue(String statusA, String statusB) throws Exception {
        state.capture(apiDriver.releaseQueue(List.of(statusA, statusB), null, null));
    }

    @Then("the release queue count is {int}")
    public void releaseQueueCount(int expected) {
        state.assertCount(expected);
    }

    @Then("the release queue by status is")
    public void releaseQueueByStatus(DataTable table) {
        state.assertByStatus(table.asMaps());
    }
}
