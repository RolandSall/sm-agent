package io.github.scrumagent.acceptance.steps.risk;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Delivery-risk steps: drive {@code /api/risk} (raw ingredients) and delegate to {@link RiskState}. The
 * active-sprint stories and the Dependency issues on their Epics are stubbed by the shared
 * {@code CommonSprintSteps} {@code Given}s.
 */
public class RiskSteps {

    private final ScrumApiDriver apiDriver;
    private final RiskState state;

    @Autowired
    public RiskSteps(ScrumApiDriver apiDriver, RiskState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @When("I request the sprint risk with committed {double} and available {double} points")
    public void requestRisk(double committed, double available) throws Exception {
        state.capture(apiDriver.risk(committed, available, null, null));
    }

    @Then("the risk stories are")
    public void riskStories(DataTable table) {
        state.assertStoriesMatch(table.asMaps());
    }

    @Then("the cross-team dependencies are")
    public void crossTeamDependencies(DataTable table) {
        state.assertDependenciesMatch(table.asMaps());
    }

    @Then("no cross-team dependencies are returned")
    public void noDependencies() {
        state.assertDependencyCount(0);
    }

    @Then("the remaining capacity is {double}")
    public void remainingCapacity(double expected) {
        state.assertRemainingCapacity(expected);
    }
}
