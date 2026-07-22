package io.github.scrumagent.acceptance.steps.dependencies;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraRows;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static io.github.scrumagent.acceptance.JiraJson.Issue.issue;

/**
 * Single-issue cross-team dependency radar steps: stub the root issue (with its Epic Link) and the
 * {@code Dependency}-type issues on that Epic, drive {@code /api/dependencies/{key}}, and delegate all
 * assertions to {@link DependencyState}.
 */
public class DependencySteps {

    private final ScrumApiDriver apiDriver;
    private final DependencyState state;

    @Autowired
    public DependencySteps(ScrumApiDriver apiDriver, DependencyState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("issue {string} is on Epic {string}")
    public void issueIsOnEpic(String issueKey, String epicKey) {
        JiraStubs.issue(issueKey, JiraJson.write(
                issue(issueKey).summary("Root").type("Story").status("In Progress", "In Progress")
                        .epicLink(epicKey).node()));
    }

    @Given("issue {string} has no Epic")
    public void issueHasNoEpic(String issueKey) {
        JiraStubs.issue(issueKey, JiraJson.write(
                issue(issueKey).summary("Root").type("Story").status("In Progress", "In Progress")
                        .node()));
    }

    @Given("the Epic {string} has these Dependency issues")
    public void theEpicHasTheseDependencyIssues(String epicKey, DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        JiraJson.Issue[] issues = rows.stream()
                .map(row -> JiraRows.toIssue(row).type("Dependency").epicLink(epicKey))
                .toArray(JiraJson.Issue[]::new);
        JiraStubs.dependencySearch(JiraJson.searchResponse(issues));
    }

    @When("I request the dependencies of {string}")
    public void requestDependencies(String issueKey) throws Exception {
        state.capture(apiDriver.dependenciesOf(issueKey));
    }

    @Then("the dependencies are")
    public void dependenciesAre(DataTable table) {
        state.assertDependenciesMatch(table.asMaps());
    }

    @Then("no cross-team dependency is reported")
    public void noneReported() {
        state.assertNoDependencies();
    }

    @Then("the reporting team is {string}")
    public void reportingTeamIs(String team) {
        state.assertOurTeam(team);
    }
}
