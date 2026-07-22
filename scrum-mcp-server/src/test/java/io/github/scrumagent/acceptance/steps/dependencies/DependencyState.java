package io.github.scrumagent.acceptance.steps.dependencies;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.dependencies.DependencyReport;
import io.github.scrumagent.jira.TeamDependency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario-scoped state and assertions for the cross-team dependency radar. Parses the captured API
 * result into a {@link DependencyReport} of {@link TeamDependency} ingredients and matches them
 * against a DataTable — arithmetic/facts only (key, blockingTeam, status, epicKey, deliverySprint).
 */
@Component
@ScenarioScope
public class DependencyState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public DependencyState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private DependencyReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, DependencyReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read dependency report", e);
        }
    }

    public void assertNoDependencies() {
        assertThat(report().dependencies()).isEmpty();
    }

    public void assertOurTeam(String expected) {
        assertThat(report().ourTeam()).as("reporting team (ourTeam)").isEqualTo(expected);
    }

    /**
     * Compares the reported dependencies against an expected DataTable. The report must hold exactly
     * as many entries as rows; each row is matched by {@code key} and ONLY the columns present are
     * asserted: {@code blockingTeam}, {@code status}, {@code epicKey}, {@code deliverySprint}.
     */
    public void assertDependenciesMatch(List<Map<String, String>> rows) {
        List<TeamDependency> deps = report().dependencies();
        assertThat(deps).as("reported dependency count").hasSize(rows.size());
        for (Map<String, String> row : rows) {
            String key = row.get("key");
            TeamDependency dep = deps.stream()
                    .filter(d -> d.key().equals(key))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No dependency with key " + key));
            compare(row, "blockingTeam", key, dep.blockingTeam());
            compare(row, "waitingTeam", key, dep.waitingTeam());
            compare(row, "status", key, dep.status());
            compare(row, "epicKey", key, dep.epicKey());
            compare(row, "deliverySprint", key, dep.deliverySprint());
        }
    }

    private static void compare(Map<String, String> row, String column, String key, String actual) {
        String expected = row.get(column);
        if (expected != null && !expected.isBlank()) {
            assertThat(actual).as("%s %s", key, column).isEqualTo(expected.trim());
        }
    }
}
