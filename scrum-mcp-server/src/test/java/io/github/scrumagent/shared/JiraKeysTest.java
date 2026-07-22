package io.github.scrumagent.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the shared Jira-key helpers used by both the risk and dependency handlers. */
class JiraKeysTest {

    @Test
    void projectOfExtractsPrefix() {
        assertThat(JiraKeys.projectOf("PROJ-123")).isEqualTo("PROJ");
    }

    @Test
    void projectOfIsNullEmptyAndDashLessSafe() {
        assertThat(JiraKeys.projectOf(null)).isEmpty();
        assertThat(JiraKeys.projectOf("")).isEmpty();
        assertThat(JiraKeys.projectOf("   ")).isEmpty();
        // No dash: return the whole key rather than throwing.
        assertThat(JiraKeys.projectOf("NOKEY")).isEqualTo("NOKEY");
        // Multi-dash: split on the last dash.
        assertThat(JiraKeys.projectOf("MY-TEAM-42")).isEqualTo("MY-TEAM");
    }
}
