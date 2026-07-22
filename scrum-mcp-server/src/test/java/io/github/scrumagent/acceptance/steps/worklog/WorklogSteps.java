package io.github.scrumagent.acceptance.steps.worklog;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Logged-hours steps: drive {@code /api/worklog} (optionally with the assignee filter or the Tier-2
 * hours-per-day divisor) and delegate assertions to {@link WorklogState}. Scenario input issues are
 * stubbed by the shared {@code the active sprint contains the following issues} table step.
 */
public class WorklogSteps {

    private final ScrumApiDriver apiDriver;
    private final WorklogState state;

    @Autowired
    public WorklogSteps(ScrumApiDriver apiDriver, WorklogState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @When("I request the workload report")
    public void requestWorkload() throws Exception {
        state.capture(apiDriver.worklog(null, null));
    }

    @When("I request the workload report filtered to assignee {string}")
    public void requestWorkloadForAssignee(String assignee) throws Exception {
        state.capture(apiDriver.worklog(null, assignee));
    }

    @When("I request the workload report at {int} hours per day")
    public void requestWorkloadAtHoursPerDay(int hoursPerDay) throws Exception {
        state.capture(apiDriver.worklog(null, null, (double) hoursPerDay));
    }

    @Then("the workload per assignee is")
    public void workloadPerAssigneeIs(DataTable table) {
        state.assertWorkloadMatches(table.asMaps());
    }

    @Then("the workload totals are")
    public void workloadTotalsAre(DataTable table) {
        state.assertWorkloadTotals(table.asMaps().get(0));
    }
}
