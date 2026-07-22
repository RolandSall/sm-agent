package io.github.scrumagent.acceptance.steps.dependencies;

import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.driver.ScrumApiDriver;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sprint-wide dependency-radar steps: drive {@code /api/dependencies} and reuse {@link DependencyState}
 * (shared, scenario-scoped) so the {@code Then} steps in {@link DependencySteps} assert the result.
 * The active-sprint stories and the Dependency issues on their Epics are stubbed by the shared
 * {@code CommonSprintSteps} {@code Given}s.
 */
public class SprintDependencySteps {

    private final ScrumApiDriver apiDriver;
    private final DependencyState state;

    @Autowired
    public SprintDependencySteps(ScrumApiDriver apiDriver, DependencyState state) {
        this.apiDriver = apiDriver;
        this.state = state;
    }

    @When("I request the sprint dependency radar")
    public void requestSprintRadar() throws Exception {
        state.capture(apiDriver.sprintDependencies());
    }
}
