package io.github.scrumagent.acceptance.steps.publication;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.scrumagent.acceptance.JiraJson;
import io.github.scrumagent.acceptance.JiraStubs;
import io.github.scrumagent.metrics.CommitMetricsPublicationCommand;
import io.github.scrumagent.metrics.PrepareMetricsPublicationQuery;
import io.github.springmediator.mediator.bus.MediatorBus;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Metrics-publication-governance steps. The publish COMMIT is a write and is deliberately not on the
 * REST API, so this feature drives the {@link MediatorBus} directly (prepare + commit) while
 * {@link PublicationState} owns the assertions.
 */
public class PublicationSteps {

    private final MediatorBus mediator;
    private final PublicationState state;

    @Autowired
    public PublicationSteps(MediatorBus mediator, PublicationState state) {
        this.mediator = mediator;
        this.state = state;
    }

    @Given("the sprint metrics have no closed or active sprints")
    public void noSprints() {
        // prepare_publish_metrics fans out to velocity (Jira's chart) and scope (active sprint).
        JiraStubs.velocity(1, JiraJson.velocityResponse());
        JiraStubs.activeSprints(1, JiraJson.sprintListResponse());
    }

    @When("I prepare a metrics publication")
    public void prepare() {
        state.capturePreview(mediator.query(new PrepareMetricsPublicationQuery()));
    }

    @When("I commit a metrics publication with token {string}")
    public void commit(String token) {
        state.captureThrown(catchThrowable(() -> mediator.send(new CommitMetricsPublicationCommand(token))));
    }

    @Then("a non-null publication token is returned")
    public void tokenReturned() {
        state.assertTokenReturned();
    }

    @Then("no POST or PUT request was made to Jira")
    public void noWrites() {
        state.assertNoWritesToJira();
    }

    @Then("the commit is refused with an IllegalStateException")
    public void refused() {
        state.assertCommitRefused();
    }
}
