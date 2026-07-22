package io.github.scrumagent.acceptance.steps.estimatevariance;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Estimate-variance steps: drive {@code /api/estimate-variance} (optionally with the min-estimate
 * fact-floor) and delegate assertions to {@link EstimateVarianceState}. Scenario input issues are
 * stubbed by the shared {@code the active sprint contains the following issues} table step.
 */
public class EstimateVarianceSteps {

    private final ScrumApiDriver apiDriver;
    private final EstimateVarianceState state;

    @Autowired
    public EstimateVarianceSteps(ScrumApiDriver apiDriver, EstimateVarianceState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @When("I request the estimate-variance report")
    public void requestVariance() throws Exception {
        state.capture(apiDriver.estimateVariance(null));
    }

    @When("I request the estimate-variance report with a minimum estimate of {int} hours")
    public void requestVarianceWithFloor(int minEstimateHours) throws Exception {
        state.capture(apiDriver.estimateVariance(minEstimateHours));
    }

    @Then("the estimate variance per issue is")
    public void estimateVariancePerIssueIs(DataTable table) {
        state.assertVarianceMatches(table.asMaps());
    }

    @Then("issue {string} returned status is {string}")
    public void returnedStatusIs(String key, String returned) {
        state.assertReturned(key, Boolean.parseBoolean(returned.trim()));
    }
}
