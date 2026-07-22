package io.github.scrumagent.acceptance.steps.velocity;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

/** Sprint-velocity steps: stub Jira's velocity chart, drive {@code /api/velocity}, delegate to {@link VelocityState}. */
public class VelocitySteps {

    private final ScrumApiDriver apiDriver;
    private final VelocityState state;

    @Autowired
    public VelocitySteps(ScrumApiDriver apiDriver, VelocityState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @Given("there are 2 closed sprints completing 8 and 12 points")
    public void twoClosedSprints() {
        // Jira's own velocity chart (greenhopper): Sprint 1 completed 8, Sprint 2 completed 12.
        JiraStubs.velocity(1, JiraJson.velocityResponse(
                JiraJson.vel(101, "Sprint 1", 8, 8),
                JiraJson.vel(102, "Sprint 2", 12, 12)));
    }

    @Given("there are 3 closed sprints completing 6, 8 and 10 points")
    public void threeClosedSprints() {
        // Most-recent sprint is last; the handler averages the trailing N.
        JiraStubs.velocity(1, JiraJson.velocityResponse(
                JiraJson.vel(101, "Sprint 1", 6, 6),
                JiraJson.vel(102, "Sprint 2", 8, 8),
                JiraJson.vel(103, "Sprint 3", 10, 10)));
    }

    @When("I request the velocity report")
    public void requestVelocity() throws Exception {
        state.capture(apiDriver.velocity(null));
    }

    @When("I request the velocity report for the last {int} sprints")
    public void requestVelocityForLastN(int sprints) throws Exception {
        state.capture(apiDriver.velocity(sprints));
    }

    @Then("the velocity summary is")
    public void velocitySummary(DataTable table) {
        state.assertVelocitySummary(table.asMaps().get(0));
    }
}
