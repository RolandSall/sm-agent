package io.github.scrumagent.acceptance.steps.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.flow.SprintFlowReport;
import io.github.scrumagent.flow.SprintFlowReport.FlowIssue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the sprint-flow feature — raw flat list, no buckets. */
@Component
@ScenarioScope
public class SprintFlowState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public SprintFlowState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private SprintFlowReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, SprintFlowReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read sprint-flow report", e);
        }
    }

    private FlowIssue issue(String key) {
        return report().issues().stream()
                .filter(i -> i.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No flow issue " + key));
    }

    /** Each row's raw fields are matched by key (only present columns are checked). */
    public void assertIssuesMatch(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            FlowIssue i = issue(row.get("key"));
            if (present(row, "statusCategory")) {
                assertThat(i.statusCategory()).as("%s statusCategory", i.key())
                        .isEqualTo(row.get("statusCategory").trim());
            }
            if (present(row, "status")) {
                assertThat(i.status()).as("%s status", i.key()).isEqualTo(row.get("status").trim());
            }
            if (present(row, "ageInStatusDays")) {
                assertLongEquals(i.ageInStatusDays(), row.get("ageInStatusDays").trim(),
                        i.key() + " ageInStatusDays");
            }
            if (present(row, "daysSinceUpdated")) {
                assertLongEquals(i.daysSinceUpdated(), row.get("daysSinceUpdated").trim(),
                        i.key() + " daysSinceUpdated");
            }
            if (present(row, "reopenCount")) {
                assertThat(i.reopenCount()).as("%s reopenCount", i.key())
                        .isEqualTo(Integer.parseInt(row.get("reopenCount").trim()));
            }
        }
    }

    public void assertIssueCount(int expected) {
        assertThat(report().issues()).as("flow issue count").hasSize(expected);
    }

    public void assertInList(String key) {
        assertThat(report().issues())
                .as("expected %s in the flow list", key)
                .anyMatch(i -> i.key().equals(key));
    }

    public void assertNullAge(String key) {
        assertThat(issue(key).ageInStatusDays()).as("%s ageInStatusDays", key).isNull();
    }

    private static void assertLongEquals(Long actual, String expected, String label) {
        if ("null".equalsIgnoreCase(expected)) {
            assertThat(actual).as(label).isNull();
        } else {
            assertThat(actual).as(label).isEqualTo(Long.parseLong(expected));
        }
    }

    private static boolean present(Map<String, String> row, String column) {
        String v = row.get(column);
        return v != null && !v.isBlank();
    }
}
