package io.github.scrumagent.acceptance.steps.scopechange;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import io.github.scrumagent.metrics.ScopeChangeReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Scenario-scoped state and assertions for the scope-change feature. */
@Component
@ScenarioScope
public class ScopeChangeState {

    private final ObjectMapper objectMapper;
    private ResultActions result;

    @Autowired
    public ScopeChangeState(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void capture(ResultActions result) {
        this.result = result;
    }

    private ScopeChangeReport report() {
        try {
            String body = result.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(body, ScopeChangeReport.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read scope-change report", e);
        }
    }

    /**
     * Compares the raw scope-change ingredients against a single expected row, checking ONLY the
     * columns present: {@code sprint} and the deterministic changelog GROUP+SUM totals
     * {@code committedPoints} (committed-at-start), {@code addedPoints}, {@code removedPoints}. No
     * verdict/churn column is asserted — that reasoning lives in the sprint-metrics skill.
     */
    public void assertScopeSummary(Map<String, String> row) {
        ScopeChangeReport report = report();
        String expectedSprint = row.get("sprint");
        if (expectedSprint != null && !expectedSprint.isBlank()) {
            assertThat(report.sprint()).as("sprint").isEqualTo(expectedSprint.trim());
        }
        compare(row, "committedPoints", "committedAtStart", report.committedAtStart());
        compare(row, "addedPoints", "addedPoints", report.addedPoints());
        compare(row, "removedPoints", "removedPoints", report.removedPoints());
    }

    /**
     * Asserts each expected {@code | key | bucket | storyPoints |} row is present in the matching
     * list with the given raw story points, where {@code bucket} is {@code added} or {@code removed}.
     */
    public void assertScopeItems(List<Map<String, String>> rows) {
        ScopeChangeReport report = report();
        for (Map<String, String> row : rows) {
            String key = row.get("key");
            String bucket = row.get("bucket").trim();
            double points = Double.parseDouble(row.get("storyPoints").trim());
            List<ScopeChangeReport.ScopeChangeItem> list = switch (bucket) {
                case "added" -> report.added();
                case "removed" -> report.removed();
                default -> throw new AssertionError("Unknown scope-change bucket: " + bucket);
            };
            assertThat(list).as("%s in %s with %s points", key, bucket, points)
                    .anyMatch(i -> i.key().equals(key) && i.storyPoints() == points);
        }
    }

    private static void compare(Map<String, String> row, String column, String label, double actual) {
        String expected = row.get(column);
        if (expected != null && !expected.isBlank()) {
            assertThat(actual).as(label).isEqualTo(Double.parseDouble(expected.trim()));
        }
    }
}
