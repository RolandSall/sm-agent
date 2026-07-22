package io.github.scrumagent.acceptance.steps.storiestotest;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static io.github.scrumagent.acceptance.JiraJson.Issue.issue;

/** Stories-to-test steps: stub the status search, drive {@code /api/stories-to-test}, delegate to state. */
public class StoriesToTestSteps {

    private final ScrumApiDriver apiDriver;
    private final StoriesToTestState state;
    private final List<JiraJson.Issue> issues = new ArrayList<>();

    @Autowired
    public StoriesToTestSteps(ScrumApiDriver apiDriver, StoriesToTestState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("a story {string} summary {string} assignee {string} with AC {string} under epic {string} in status {string}")
    public void aStory(String key, String summary, String assignee, String ac, String epic, String status) {
        issues.add(issue(key).summary(summary).assignee(assignee)
                .acceptanceCriteria(ac).epicLink(epic).status(status, "In Progress"));
    }

    @When("I request the stories to test with the default statuses")
    public void requestDefault() throws Exception {
        JiraStubs.storiesToTestSearch(JiraJson.searchResponse(issues.toArray(new JiraJson.Issue[0])));
        state.capture(apiDriver.storiesToTest(null, null, null));
    }

    @When("I request the stories to test with statuses {string} and {string}")
    public void requestCustom(String statusA, String statusB) throws Exception {
        JiraStubs.storiesToTestSearch(JiraJson.searchResponse(issues.toArray(new JiraJson.Issue[0])));
        state.capture(apiDriver.storiesToTest(List.of(statusA, statusB), null, null));
    }

    @Then("the stories to test are")
    public void storiesToTestAre(DataTable table) {
        state.assertStoriesMatch(table.asMaps());
    }

    @Then("the stories to test list does not contain {string}")
    public void listDoesNotContain(String key) {
        state.assertDoesNotContain(key);
    }
}
