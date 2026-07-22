package io.github.scrumagent.acceptance.steps.risk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.jira.TeamDependency;
import io.github.scrumagent.risk.RiskReport;
import io.github.scrumagent.risk.StoryRisk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the delivery-risk feature — raw ingredients only. */
@Component
@ScenarioScope
public class RiskState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public RiskState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private RiskReport report() {
        try {
            return objectMapper.readValue(result.andReturn().getResponse().getContentAsString(),
                    RiskReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read risk report", e);
        }
    }

    /** The sprint stories are returned with their raw facts, matched by key (only present columns checked). */
    public void assertStoriesMatch(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String key = row.get("key");
            StoryRisk s = report().stories().stream().filter(x -> x.key().equals(key)).findFirst()
                    .orElseThrow(() -> new AssertionError("No story " + key));
            if (present(row, "epicKey")) {
                assertThat(s.epicKey()).as("%s epicKey", key).isEqualTo(row.get("epicKey").trim());
            }
            if (present(row, "storyPoints")) {
                assertThat(s.storyPoints()).as("%s storyPoints", key)
                        .isEqualTo(Double.parseDouble(row.get("storyPoints").trim()));
            }
            if (present(row, "unestimated")) {
                assertThat(s.unestimated()).as("%s unestimated", key)
                        .isEqualTo(Boolean.parseBoolean(row.get("unestimated").trim()));
            }
        }
    }

    /** The cross-team Dependencies on the sprint's Epics are returned RAW (unfiltered), matched by key. */
    public void assertDependenciesMatch(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String key = row.get("key");
            TeamDependency d = report().dependencies().stream().filter(x -> x.key().equals(key)).findFirst()
                    .orElseThrow(() -> new AssertionError("No dependency " + key));
            if (present(row, "blockingTeam")) {
                assertThat(d.blockingTeam()).as("%s blockingTeam", key).isEqualTo(row.get("blockingTeam").trim());
            }
            if (present(row, "waitingTeam")) {
                assertThat(d.waitingTeam()).as("%s waitingTeam", key).isEqualTo(row.get("waitingTeam").trim());
            }
            if (present(row, "status")) {
                assertThat(d.status()).as("%s status", key).isEqualTo(row.get("status").trim());
            }
            if (present(row, "epicKey")) {
                assertThat(d.epicKey()).as("%s epicKey", key).isEqualTo(row.get("epicKey").trim());
            }
        }
    }

    public void assertDependencyCount(int expected) {
        assertThat(report().dependencies()).as("dependency count").hasSize(expected);
    }

    public void assertRemainingCapacity(Double expected) {
        assertThat(report().remainingCapacityPoints()).isEqualTo(expected);
    }

    private static boolean present(Map<String, String> row, String column) {
        String v = row.get(column);
        return v != null && !v.isBlank();
    }
}
