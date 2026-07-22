package io.github.scrumagent.acceptance.steps.publication;

import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.acceptance.MockServerSupport;
import io.github.scrumagent.metrics.MetricsPublicationPreview;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario-scoped state and assertions for the metrics-publication-governance feature. Holds the
 * prepared preview and any throwable from a refused commit, and owns every assertion — including the
 * MockServer verification that no write ever reached Jira.
 */
@Component
@ScenarioScope
public class PublicationState {

    private MetricsPublicationPreview preview;
    private Throwable thrown;

    public void capturePreview(MetricsPublicationPreview preview) {
        this.preview = preview;
    }

    public void captureThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    public void assertTokenReturned() {
        assertThat(preview).isNotNull();
        assertThat(preview.token()).isNotBlank();
    }

    public void assertNoWritesToJira() {
        MockServerSupport.server().verify(HttpRequest.request().withMethod("POST"), VerificationTimes.exactly(0));
        MockServerSupport.server().verify(HttpRequest.request().withMethod("PUT"), VerificationTimes.exactly(0));
    }

    public void assertCommitRefused() {
        assertThat(thrown).isNotNull();
        assertThat(hasCause(thrown, IllegalStateException.class))
                .as("expected an IllegalStateException in the cause chain of %s", thrown)
                .isTrue();
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable current = t; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
