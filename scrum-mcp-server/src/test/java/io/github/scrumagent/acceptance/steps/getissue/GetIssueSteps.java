package io.github.scrumagent.acceptance.steps.getissue;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

import static io.github.scrumagent.acceptance.JiraJson.Issue.issue;

/** Get-issue steps: stub a single issue, drive {@code /api/issue/{key}}, delegate to {@link GetIssueState}. */
public class GetIssueSteps {

    private final ScrumApiDriver apiDriver;
    private final GetIssueState state;

    @Autowired
    public GetIssueSteps(ScrumApiDriver apiDriver, GetIssueState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("issue {string} exists with summary {string}, status {string}, {int} story points, label {string} and a Blocks link to {string}")
    public void issueExists(String key, String summary, String status, int storyPoints,
                            String label, String linkedKey) {
        JiraStubs.issue(key, JiraJson.write(
                issue(key).summary(summary).type("Story").status(status, status)
                        .assignee("Alice").storyPoints(storyPoints).labels(label)
                        .outwardLink("Blocks", linkedKey, "To Do").node()));
    }

    @When("I request issue {string}")
    public void requestIssue(String key) throws Exception {
        state.capture(apiDriver.issue(key));
    }

    @Then("the issue is")
    public void theIssueIs(DataTable table) {
        state.assertIssueMatches(table.asMaps().get(0));
    }

    @Then("the issue has label {string}")
    public void hasLabel(String label) {
        state.assertLabel(label);
    }

    @Then("the issue has a {string} link to {string}")
    public void hasLink(String type, String linkedKey) {
        state.assertLink(type, linkedKey);
    }
}
